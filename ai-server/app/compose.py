import json
import os
from typing import Callable
from langchain_core.messages import HumanMessage, SystemMessage
from langchain_openai import ChatOpenAI
from .schemas import CourseComposeRequest, PolishedCourse
from .search import haversine_km

STAY_MINUTES = {"ANCHOR": 90, "MEAL": 60, "FUN": 90, "MARKET": 60}   # 가이드 4-1 기본 체류
SPEED_KMH = {"PUBLIC_TRANSPORT": 15, "CAR": 25}
SLOT_LABEL = {"ANCHOR": "대표 관광지", "MEAL": "식사", "FUN": "즐길거리", "MARKET": "시장"}


# ---------- 1단계: RAG 검색 결과로 코스 조합 (OpenAI 사용 안 함) ----------

def decide_slots(candidates: dict[str, list]) -> list[str]:
    """후보 수로 3개/4개 코스를 정한다 (가이드 4-1 규칙)."""
    fun, market = len(candidates.get("FUN", [])), len(candidates.get("MARKET", []))
    if not market:
        return ["MEAL", "FUN"] if fun else ["MEAL"]
    return ["MEAL", "FUN", "MARKET"] if fun >= 2 else ["MEAL", "MARKET"]


def choose_places(candidates: dict[str, list], slots: list[str]) -> dict[str, dict]:
    """슬롯마다 점수 1등을 고른다. 후보는 이미 점수순으로 정렬돼 있다."""
    chosen, used = {}, set()
    for slot in slots:
        best = next(c for c in candidates[slot] if c["placeId"] not in used)
        chosen[slot] = best
        used.add(best["placeId"])
    return chosen


def hhmm(minutes: int) -> str:
    return f"{minutes // 60:02d}:{minutes % 60:02d}"


def move_minutes(a: dict, b: dict, transport: str) -> int:
    """이동시간 추정: 직선거리 × 1.3 ÷ 속도. 최소 5분."""
    km = haversine_km(a["lat"], a["lng"], b["lat"], b["lng"]) * 1.3
    return max(5, round(km / SPEED_KMH[transport] * 60))


def schedule(stops: list[dict], start: str, transport: str) -> None:
    """stops 순서대로 도착·출발 시간을 채운다. stops[0]=ANCHOR, stops[1]=MEAL 이다."""
    h, m = map(int, start.split(":"))
    stay = {i: STAY_MINUTES[s["slot"]] for i, s in enumerate(stops)}
    # 점심이 11:30보다 일찍 시작하면 앵커에 더 머문다
    meal_start = h * 60 + m + stay[0] + move_minutes(stops[0]["place"], stops[1]["place"], transport)
    if meal_start < 11 * 60 + 30:
        stay[0] += 11 * 60 + 30 - meal_start

    now = h * 60 + m
    for i, stop in enumerate(stops):
        move = move_minutes(stops[i - 1]["place"], stop["place"], transport) if i > 0 else 0
        now += move
        stop |= {"moveMinutes": move, "startTime": hhmm(now), "endTime": hhmm(now + stay[i])}
        now += stay[i]


def intro_of(document: str) -> str:
    """장소 문서에서 소개 부분만 꺼낸다. 없으면 빈 문자열."""
    return document.split("소개:", 1)[1].strip() if "소개:" in document else ""


def draft_text(stop: dict) -> str:
    """OpenAI 없이 만드는 기본 소개 문장 (초안이자, 다듬기에 실패했을 때 그대로 쓰는 문장)."""
    place = stop["place"]
    where = f"앞 장소에서 이동 {stop['moveMinutes']}분" if stop["moveMinutes"] else "코스 시작"
    intro = intro_of(place.get("document", ""))[:60]
    return f"{SLOT_LABEL[stop['slot']]} · {where}. {intro}".strip()


def build_draft(request: CourseComposeRequest) -> dict:
    """RAG 검색 결과(anchor, candidates)만으로 코스 초안을 만든다."""
    slots = decide_slots(request.candidates)
    chosen = choose_places(request.candidates, slots)
    stops = [{"slot": "ANCHOR", "place": request.anchor}] + [{"slot": s, "place": chosen[s]} for s in slots]
    schedule(stops, request.startTime, request.transportType)
    for stop in stops:
        stop["description"] = draft_text(stop)
    name = request.anchor.get("name") or "추천"
    return {
        "title": f"{name} 주변 코스"[:20],
        "summary": " → ".join(s["place"].get("name", "") for s in stops),
        "stops": stops,
    }


# ---------- 2단계: OpenAI(LangChain)로 응답 문장 생성 ----------

def polish_with_openai(draft: dict) -> PolishedCourse:
    """초안의 제목·요약·장소 소개 문장만 자연스럽게 다듬는다. 장소·순서·시간은 바꾸지 않는다."""
    if not os.getenv("OPENAI_API_KEY", "").strip():
        raise RuntimeError("OPENAI_API_KEY가 설정되지 않았습니다.")
    model = ChatOpenAI(model=os.getenv("OPENAI_MODEL", "gpt-4o-mini"), timeout=30, max_retries=0, use_responses_api=True, store=False)
    chain = model.with_structured_output(PolishedCourse, method="json_schema", strict=True)
    instruction = """너는 여행 코스 문장을 다듬는 편집자다.
    주어진 코스 초안의 제목, 요약, 장소별 소개 문장을 여행자가 읽기 좋게 자연스러운 한국어로 다듬어라.
    장소를 추가·삭제·교체하거나 순서를 바꾸지 마라. placeId는 받은 값 그대로 같은 순서로 돌려줘라.
    초안과 장소 정보에 없는 사실(운영시간, 가격, 맛 평가, 거리)은 새로 쓰지 마라."""
    payload = {
        "제목": draft["title"],
        "요약": draft["summary"],
        "장소": [{"placeId": s["place"]["placeId"], "이름": s["place"].get("name"), "구분": SLOT_LABEL[s["slot"]],
                 "시간": f"{s['startTime']}~{s['endTime']}", "초안": s["description"],
                 "장소정보": intro_of(s["place"].get("document", ""))[:300]} for s in draft["stops"]],
    }
    return chain.invoke([SystemMessage(content=instruction), HumanMessage(content=json.dumps(payload, ensure_ascii=False))])


def apply_polish(draft: dict, polished: PolishedCourse | None) -> bool:
    """다듬은 문장이 규칙을 지키면 초안에 덮어쓴다. 장소가 바뀌었으면 초안 문장을 그대로 둔다."""
    if polished is None:
        return False
    if [p.placeId for p in polished.stops] != [s["place"]["placeId"] for s in draft["stops"]]:
        return False
    if not polished.title.strip() or any(not p.description.strip() for p in polished.stops):
        return False
    draft["title"] = polished.title.strip()[:20]
    draft["summary"] = polished.summary.strip()[:120] or draft["summary"]
    for stop, p in zip(draft["stops"], polished.stops):
        stop["description"] = p.description.strip()[:80]
    return True


def to_response(draft: dict, is_polished: bool) -> dict:
    """Spring으로 보낼 최종 JSON. 내부용 place(벡터·문서)는 빼고 필요한 값만 담는다."""
    stops = [{"slot": s["slot"], "placeId": s["place"]["placeId"], "name": s["place"].get("name"),
              "moveMinutes": s["moveMinutes"], "startTime": s["startTime"], "endTime": s["endTime"],
              "description": s["description"]} for s in draft["stops"]]
    return {"title": draft["title"], "summary": draft["summary"], "stops": stops,
            "polished": is_polished, "routeSource": "ESTIMATED"}
