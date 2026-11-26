"""
숙소 기반 일정 추천 — 기존 코스가 없을 때 숙소 주변 관광지로 숙소→관광지→숙소 일정을 짠다.
경로 최적화는 하지 않고, 거리순으로 가져온 후보를 하루 3곳씩 그대로 잘라 배분한다
(course_rag.recommend_lodging_itinerary를 그대로 옮긴 로직).

독립적인 기능이라 /internal/dialogue의 조건 대화 그래프와 분리된 별도 API
(/internal/lodging-itinerary)로 둔다(설계 결정 3). 숙소 조회와 주변 장소 조회를 이제
ai-server가 직접 하므로, 응답의 recommendedPlaces에 장소명·주소를 바로 포함한다 —
스프링이 자신이 가져온 후보 목록과 다시 대조해 채울 필요가 없다.
"""
from datetime import date, timedelta

from . import db

PER_DAY = 3


def _reorder_by_preferences(places, preferences):
    """선호어가 이름·주소에 있는 장소를 앞으로 보낸다. ConditionDialogueService.places()와 동일한 규칙."""
    terms = [p.strip() for p in (preferences or []) if p and p.strip()]
    if not terms:
        return places
    matched, rest = [], []
    for place in places:
        text = f"{place.get('name', '')} {place.get('address', '')}"
        (matched if any(term in text for term in terms) else rest).append(place)
    return matched + rest


def build(conditions, conn=None):
    """
    conditions: {"lodgingId": int, "durationDays": int|None, "startDate": str|None, "preferences": [str]}
    반환: (message, recommendedPlaces). recommendedPlaces가 None이면 추천하지 못했다는 뜻이고
    message에 이유가 담긴다.
    """
    lodging_id = conditions.get("lodgingId")
    conn = conn or db.connect()
    lodging = db.lodging_by_id(conn, lodging_id)
    if lodging is None:
        return "예약 화면에서 이용 가능한 숙소를 다시 선택해 주세요.", None
    if lodging.get("latitude") is None or lodging.get("longitude") is None:
        return "주변 후보가 부족해요. 반경을 넓히거나 다른 조건으로 다시 시도해 주세요.", None

    places = db.places_near_lodging(conn, lodging["latitude"], lodging["longitude"])
    places = _reorder_by_preferences(places, conditions.get("preferences"))

    duration = conditions.get("durationDays") or 1
    needed = PER_DAY * duration
    if len(places) < min(3, needed):
        return "주변 후보가 부족해요. 반경을 넓히거나 다른 조건으로 다시 시도해 주세요.", None

    chosen = places[:needed]
    start = date.fromisoformat(conditions["startDate"]) if conditions.get("startDate") else None
    lines = ["숙소 주변 관광지로 짠 일정이에요."]
    day_chunks = [chosen[i:i + PER_DAY] for i in range(0, len(chosen), PER_DAY)]
    recommended = []
    for day_no, day_places in enumerate(day_chunks, 1):
        header = f"{day_no}일차" + (f" ({(start + timedelta(days=day_no - 1)).isoformat()})" if start else "")
        lines.append(header)
        for seq, place in enumerate(day_places, 1):
            lines.append(f"{seq}. {place['name']} — {place.get('address', '')}")
            recommended.append({
                "dayNo": day_no, "seq": seq, "placeId": place["placeId"],
                "name": place["name"], "address": place.get("address", ""),
            })
    return "\n".join(lines), recommended
