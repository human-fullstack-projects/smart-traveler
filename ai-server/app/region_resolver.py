"""
지역명(자유 텍스트) -> Chroma destination 메타데이터 매칭.
research/chat_test.py의 resolve_destination을 그대로 포팅했다. RDB(region 테이블)의 롤업 쿼리
대신, Chroma에 실제로 색인된 destination 문자열 집합과 부분일치시킨다 — "부산"처럼 시/도만
말해도 하위 시군구 destination(예: "부산광역시 해운대구")을 전부 묶어 롤업 효과를 낸다.
스프링은 더 이상 지역명을 해석하지 않고 원문 그대로 넘기므로, 이 해석은 전부 ai-server가 한다.
"""
from . import vector_store


def known_destinations(collection=None):
    """Chroma 컬렉션에 실제로 색인된 destination 값 전체(중복 제거, 정렬)."""
    collection = collection or vector_store.get_collection()
    all_meta = collection.get(include=["metadatas"])["metadatas"]
    return sorted({m["destination"] for m in all_meta if m.get("destination")})


def resolve_destination(name, destinations=None, collection=None):
    """
    name: 사용자가 말한 지역명(예: "부산", "부산 해운대구"). 비어 있거나 매칭이 없으면 None —
    이 경우 지역 필터 없이 검색한다(순수 자유서술 질의와 동일하게 처리, chat_test.py와 동일한 동작).
    destinations: 미리 구해둔 known_destinations 목록(테스트·재사용 시 주입). 없으면 Chroma에서 구한다.
    반환: 정확히 하나 매칭되면 str, 여러 개면 {"$in": [...]}, 매칭 없으면 None.
    """
    if not name:
        return None
    pool = destinations if destinations is not None else known_destinations(collection)
    matches = [d for d in pool if name in d]
    if not matches:
        return None
    # "제주"처럼 시군구 없이 통짜로만 적힌 destination은 내용이 거의 없는 저정보 row일 수 있어,
    # 시군구가 붙은 구체적인 매칭이 있으면 그걸 우선한다.
    detailed = [d for d in matches if d != name]
    chosen = detailed or matches
    if len(chosen) == 1:
        return chosen[0]
    return {"$in": chosen}
