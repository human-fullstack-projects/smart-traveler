import math
from . import rag

# 슬롯 템플릿: 코스의 뼈대를 데이터로 관리한다 (가이드 4-1)
SLOTS = {
    "ANCHOR": {"types": ["ATTRACTION", "CULTURE"], "query": "대표 관광 명소"},
    "MEAL": {"types": ["RESTAURANT"], "query": "점심 식사하기 좋은 맛집"},
    "FUN": {"types": ["LEISURE", "CULTURE", "ATTRACTION"], "query": "체험하거나 즐길 거리"},
    "MARKET": {"market_only": True, "query": "전통시장 먹거리 구경"},
}
RADIUS_KM = {"PUBLIC_TRANSPORT": 3.0, "CAR": 10.0}
WEIGHTS = {"similarity": 0.4, "closeness": 0.4, "popularity": 0.2}  # 1차 버전 시작값


def haversine_km(lat1: float, lng1: float, lat2: float, lng2: float) -> float:
    r = 6371.0
    p1, p2 = math.radians(lat1), math.radians(lat2)
    dp, dl = p2 - p1, math.radians(lng2 - lng1)
    a = math.sin(dp / 2) ** 2 + math.cos(p1) * math.cos(p2) * math.sin(dl / 2) ** 2
    return 2 * r * math.asin(math.sqrt(a))


def name_of(document: str) -> str:
    """문서 첫 줄 "[이름] 주소"에서 이름만 꺼낸다."""
    return document[1:document.index("]")] if document.startswith("[") and "]" in document else ""


def to_place(meta: dict, doc: str) -> dict:
    return {"placeId": meta["place_id"], "name": name_of(doc), "placeType": meta["place_type"],
            "lat": meta["lat"], "lng": meta["lng"], "popularity": meta["popularity"], "document": doc}


def find_anchor(sigungu_code: str, anchor_name: str | None, query: str, exclude_ids: set[int]) -> dict | None:
    """앵커(첫 장소)를 찾는다. 이름을 말했으면 문서 안 이름 검색이 먼저, 없으면 질의 임베딩 + 인기도."""
    where = {"$and": [{"sigungu_code": sigungu_code}, {"place_type": {"$in": SLOTS["ANCHOR"]["types"]}}]}
    if anchor_name:
        res = rag.places.get(where=where, where_document={"$contains": anchor_name}, include=["metadatas", "documents"])
        found = [to_place(m, d) for m, d in zip(res["metadatas"], res["documents"]) if m["place_id"] not in exclude_ids]
        if found:
            # 이름이 정확히 같은 곳 > 이름이 그 단어로 시작하는 곳 > 나머지, 같으면 인기도 순
            found.sort(key=lambda p: (p["name"] != anchor_name, not p["name"].startswith(anchor_name), -p["popularity"]))
            return found[0]
    res = rag.places.query(query_embeddings=[rag.get_embeddings().embed_query(query)], n_results=10,
                           where=where, include=["metadatas", "documents", "distances"])
    best = None
    for meta, doc, dist in zip(res["metadatas"][0], res["documents"][0], res["distances"][0]):
        if meta["place_id"] in exclude_ids:
            continue
        score = 0.6 * max(0.0, 1 - dist) + 0.4 * meta["popularity"]
        if best is None or score > best["score"]:
            best = to_place(meta, doc) | {"score": round(score, 3)}
    return best


def search_slot(anchor: dict, slot: str, query: str, radius_km: float, exclude_ids: set[int], k: int = 5) -> list[dict]:
    """앵커 반경 안에서 한 슬롯의 후보를 점수순으로 k개 찾는다.
    1) Chroma 메타데이터 필터(타입·시장·위경도 사각형) + 질의 임베딩 유사도로 후보를 넉넉히 받고
    2) 파이썬이 정확한 거리를 계산해 반경 밖을 버리고 점수를 매긴다."""
    dlat = radius_km / 111.0
    dlng = radius_km / (111.0 * math.cos(math.radians(anchor["lat"])))
    conds = [
        {"lat": {"$gte": anchor["lat"] - dlat}}, {"lat": {"$lte": anchor["lat"] + dlat}},
        {"lng": {"$gte": anchor["lng"] - dlng}}, {"lng": {"$lte": anchor["lng"] + dlng}},
    ]
    if SLOTS[slot].get("market_only"):
        conds.append({"is_market": True})
    else:
        conds.append({"place_type": {"$in": SLOTS[slot]["types"]}})
        conds.append({"is_market": False})  # 시장은 MARKET 슬롯에서만 쓴다
    res = rag.places.query(query_embeddings=[rag.get_embeddings().embed_query(query)], n_results=k * 4,
                           where={"$and": conds}, include=["metadatas", "documents", "distances"])
    out = []
    for meta, doc, dist in zip(res["metadatas"][0], res["documents"][0], res["distances"][0]):
        km = haversine_km(anchor["lat"], anchor["lng"], meta["lat"], meta["lng"])
        if km > radius_km or meta["place_id"] == anchor["placeId"] or meta["place_id"] in exclude_ids:
            continue
        score = (WEIGHTS["similarity"] * max(0.0, 1 - dist)          # cosine 거리 → 유사도
                 + WEIGHTS["closeness"] * (1 - km / radius_km)
                 + WEIGHTS["popularity"] * meta["popularity"])
        out.append(to_place(meta, doc) | {"slot": slot, "km": round(km, 2), "score": round(score, 3)})
    return sorted(out, key=lambda x: x["score"], reverse=True)[:k]


def search_candidates(sigungu_code: str, anchor_name: str | None, transport_type: str,
                      slots: list[str], queries: dict[str, str], exclude_ids: list[int]) -> dict:
    """앵커 1곳 + 슬롯별 후보 Top-5. 후보가 없으면 반경을 1.5배로 한 번만 넓힌다."""
    exclude = set(exclude_ids)
    anchor = find_anchor(sigungu_code, anchor_name, queries.get("ANCHOR") or SLOTS["ANCHOR"]["query"], exclude)
    if anchor is None:
        return {"anchor": None, "radiusKm": None, "candidates": {}}
    radius = RADIUS_KM.get(transport_type, RADIUS_KM["PUBLIC_TRANSPORT"])
    candidates = {}
    for slot in slots:
        if slot == "ANCHOR":
            continue
        query = queries.get(slot) or SLOTS[slot]["query"]
        found = search_slot(anchor, slot, query, radius, exclude)
        if not found:
            found = search_slot(anchor, slot, query, radius * 1.5, exclude)
        candidates[slot] = found
    return {"anchor": anchor, "radiusKm": radius, "candidates": candidates}
