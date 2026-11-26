"""
/internal/dialogue의 조회·추천 로직. research/chat_test.py를 그대로 포팅했다 — 매 메시지를
독립된 1회성 질의로 다룬다(이전 대화 상태를 들고 있지 않는다). 흐름은 설계 문서
(vector_db_design.md) 6번과 동일하다:
  ① 슬롯 추출(지역·지역테마·목적테마·기간·동행·교통·잔여 자유서술)
  ② 지역명 -> Chroma destination 메타데이터 매칭(region_resolver)
  ③ 메타데이터 필터 + 시맨틱 검색(vector_store)
  ④ 1위 코스를 PK로 RDB 재조회(db.fetch_course_detail)
"""
import json
import os

from openai import OpenAI

from . import db, region_resolver, vector_store

SLOT_MODEL = os.environ.get("SLOT_MODEL", "gpt-4o-mini")

EXTRACT_TOOL = [
    {
        "type": "function",
        "function": {
            "name": "extract_travel_slots",
            "description": "사용자의 여행 요청 문장에서 구조화된 조건을 추출한다. 언급되지 않은 항목은 생략한다.",
            "parameters": {
                "type": "object",
                "properties": {
                    "destination": {
                        "type": "string",
                        "description": "구체적 지역명(시/도/군/구 등). 언급 안 됐으면 생략",
                    },
                    "region_themes": {
                        "type": "array",
                        "items": {"type": "string", "enum": list(vector_store.REGION_THEME_SLUG.keys())},
                        "description": "지역 분위기 테마. 복수 가능",
                    },
                    "purpose_theme": {
                        "type": "string",
                        "enum": ["역사·전통", "맛집", "사찰·힐링", "쇼핑", "문화"],
                        "description": "여행 목적 테마. 온천/스파/힐링/템플스테이 요청은 '사찰·힐링'으로 분류",
                    },
                    "duration_days": {
                        "type": "integer",
                        "description": "총 여행일수. '1박2일'=2, '2박3일'=3, '당일'=1 로 환산",
                    },
                    "companion_type": {
                        "type": "string",
                        "enum": ["FAMILY", "SOLO", "FRIENDS", "COUPLE"],
                    },
                    "transport_type": {
                        "type": "string",
                        "enum": ["CAR", "PUBLIC_TRANSPORT"],
                        "description": "자동차/차=CAR, 기차/지하철/버스/대중교통=PUBLIC_TRANSPORT",
                    },
                    "residual_text": {
                        "type": "string",
                        "description": "위 슬롯으로 못 담은 분위기/세부 선호 묘사. 없으면 원문 문장 그대로 반환",
                    },
                    "is_travel_request": {
                        "type": "boolean",
                        "description": "여행 코스 추천 의도가 있으면 true. 지역·기간 등 구체 정보가 없어도 "
                                       "'추천해줘', '아무데나 가고싶어'처럼 여행 의도가 있으면 true로 판단한다. "
                                       "날씨·잡담·다른 주제 질문처럼 여행 코스 추천과 무관하면 false. 애매하면 true로 판단한다.",
                    },
                },
                "required": ["residual_text", "is_travel_request"],
            },
        },
    }
]

OFF_TOPIC_REPLY = (
    "여행 일정 추천을 도와드리는 챗봇이에요.\n"
    "가고 싶은 지역, 여행 기간, 함께 가는 사람, 이동수단, 원하는 분위기 중\n"
    "원하시는 것만 편하게 말씀해 주시면 일정을 추천해 드릴게요.\n\n"
    "예시: \"부산으로 2박3일 친구랑 바다 보러 가고 싶어\""
)


def extract_slots(message, client=None):
    client = client or OpenAI()
    resp = client.chat.completions.create(
        model=SLOT_MODEL,
        messages=[{"role": "user", "content": message}],
        tools=EXTRACT_TOOL,
        tool_choice={"type": "function", "function": {"name": "extract_travel_slots"}},
    )
    args = resp.choices[0].message.tool_calls[0].function.arguments
    return json.loads(args)


def _build_slots(raw_slots, collection):
    slots = {}
    destination = region_resolver.resolve_destination(raw_slots.get("destination"), collection=collection)
    if destination:
        slots["destination"] = destination
    if raw_slots.get("region_themes"):
        slots["region_themes"] = raw_slots["region_themes"]
    if raw_slots.get("purpose_theme"):
        slots["purpose_theme"] = raw_slots["purpose_theme"]
    if raw_slots.get("duration_days"):
        slots["duration_days"] = raw_slots["duration_days"]
    if raw_slots.get("companion_type"):
        slots["companion_type"] = raw_slots["companion_type"]
    if raw_slots.get("transport_type"):
        slots["transport_type"] = raw_slots["transport_type"]
    return slots


def _format_itinerary(detail):
    lines = [
        f"인원: 성인{detail['adults']}/아동{detail['children']} "
        f"| 동행: {detail['companion_type']} | 교통: {detail['transport_type']}"
    ]
    for day in detail["itinerary"]:
        places = " → ".join(v.get("place_name", "") for v in day.get("visits", []))
        lines.append(f"{day.get('day_no')}일차: {places}")
    return "\n".join(lines)


def _format_itinerary_brief(detail):
    lines = []
    for day in detail["itinerary"]:
        places = ", ".join(v.get("place_name", "") for v in day.get("visits", []))
        lines.append(f"{day.get('day_no')}일차 {places}")
    return " ".join(lines)


def handle(message, excluded_course_ids=None, n_results=5, conn=None, collection=None, embed_fn=None, client=None):
    """
    research/chat_test.py의 main() 루프 한 턴과 동일하다.
    반환: (message_text, recommendedCourseId, recommendedCourseIds). 결과가 없으면 뒤의 둘은 None.
    """
    excluded_course_ids = excluded_course_ids or []
    collection = collection or vector_store.get_collection()

    try:
        raw_slots = extract_slots(message, client=client)
    except Exception:
        # 추출 자체가 실패한 경우(네트워크/파싱 오류)는 "여행과 무관함"과 다른 문제이므로,
        # 차단하지 않고 원문을 그대로 검색 질의로 써서 기존 동작을 유지한다.
        raw_slots = {"residual_text": message, "is_travel_request": True}

    if not raw_slots.get("is_travel_request", True):
        return OFF_TOPIC_REPLY, None, None

    slots = _build_slots(raw_slots, collection)
    query_text = raw_slots.get("residual_text") or message

    embedding = embed_fn(query_text) if embed_fn else vector_store.embed_query(query_text)
    result, _trace = vector_store.search(embedding, slots, excluded_course_ids, n_results=n_results, collection=collection)

    ids = result["ids"][0]
    if not ids:
        return "말씀하신 조건에 맞는 코스를 찾지 못했어요. 지역이나 여행 기간을 바꿔서 다시 알려주세요.", None, None

    course_ids = [int(cid) for cid in ids]
    lines = [f"검색 결과 ({len(course_ids)}건)"]
    conn = conn or db.connect()
    details = {}
    for rank, (course_id, doc, meta, _dist) in enumerate(zip(
        course_ids, result["documents"][0], result["metadatas"][0], result["distances"][0]
    ), 1):
        detail = db.fetch_course_detail(conn, course_id)
        details[course_id] = detail
        lines.append(f"{rank}번 일정 {meta.get('destination', '')} (코스 #{course_id})")
        lines.append(_format_itinerary_brief(detail) if detail else doc[:80])

    top_course_id = course_ids[0]
    detail = details.get(top_course_id)
    if detail:
        lines.append(f"\n[추천하는 코스 - 코스 #{top_course_id}]")
        lines.append(_format_itinerary(detail))

    lines.append("\n" + OFF_TOPIC_REPLY)
    return "\n".join(lines), top_course_id, course_ids
