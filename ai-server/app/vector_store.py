"""
course 벡터DB(Chroma) 검색 모듈. research/search_courses.py를 그대로 포팅했다.

지리적 범위는 region_resolver.resolve_destination이 구한 destination 문자열(또는 $in 목록)로
건다 — RDB region 테이블 롤업은 쓰지 않는다. 메타데이터 필터 + 단계적 완화(폴백) 로직은
설계 문서(vector_db_design.md) 6번 그대로다.

필터 결과가 0건이면 아래 고정 순서로 조건을 하나씩 제거하며 재시도한다.
  1) transport_type  2) companion_type  3) duration_days  4) purpose_theme
destination·region_theme_*·이미 추천한 코스 제외(course_id $nin)는 끝까지 유지한다.
처음부터 destination/region_theme_*가 없는 순수 자유서술 질의(또는 지역명이 매칭되지 않은 질의)는
바로 필터 없이 검색한다.
"""
import os

import chromadb
from openai import OpenAI

CHROMA_PATH = os.environ.get(
    "CHROMA_PATH",
    os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "data", "chroma_db"),
)
COLLECTION_NAME = "travel_courses"
EMBEDDING_MODEL = os.environ.get("COURSE_EMBEDDING_MODEL", "text-embedding-3-small")

RELAX_ORDER = ["transport_type", "companion_type", "duration_days", "purpose_theme"]

REGION_THEME_SLUG = {
    "도시": "city",
    "바다": "sea",
    "산·숲": "mountain",
    "섬": "island",
    "시골": "rural",
    "호수·강": "lake_river",
    "어촌·포구": "fishing_village",
}


def get_collection(chroma_path=None, collection_name=COLLECTION_NAME):
    client = chromadb.PersistentClient(path=chroma_path or CHROMA_PATH)
    return client.get_collection(collection_name)


def embed_query(text, model=None, client=None):
    client = client or OpenAI()
    return client.embeddings.create(model=model or EMBEDDING_MODEL, input=[text]).data[0].embedding


def _geo_clauses(slots):
    clauses = []
    destination = slots.get("destination")
    if destination is not None:
        clauses.append({"destination": destination})
    for theme in slots.get("region_themes") or []:
        slug = REGION_THEME_SLUG.get(theme)
        if slug:
            clauses.append({f"region_theme_{slug}": True})
    return clauses


def _build_where(slots, dropped, excluded_course_ids):
    clauses = _geo_clauses(slots)
    if excluded_course_ids:
        clauses.append({"course_id": {"$nin": [int(x) for x in excluded_course_ids]}})
    for key in RELAX_ORDER:
        if key in dropped:
            continue
        value = slots.get(key)
        if value is not None:
            clauses.append({key: value})
    if not clauses:
        return None
    if len(clauses) == 1:
        return clauses[0]
    return {"$and": clauses}


def search(query_embedding, slots=None, excluded_course_ids=None, n_results=20, collection=None):
    """
    query_embedding: 이미 임베딩된 쿼리 벡터(잔여 자유서술/조건 요약 등을 호출부에서 임베딩해 넘긴다).
    slots: {"destination": str|dict|None, "region_themes": [str], "purpose_theme": str,
            "duration_days": int, "companion_type": str, "transport_type": str}
    반환: (result, trace) - trace는 어떤 조건을 제거해가며 검색했는지 기록.
    """
    slots = slots or {}
    excluded_course_ids = excluded_course_ids or []
    collection = collection or get_collection()

    has_region_identity = bool(_geo_clauses(slots))

    dropped = set()
    trace = []
    for step in range(len(RELAX_ORDER) + 1):
        where = _build_where(slots, dropped, excluded_course_ids)
        kwargs = {"query_embeddings": [query_embedding], "n_results": n_results}
        if where:
            kwargs["where"] = where
        result = collection.query(**kwargs)
        count = len(result["ids"][0])
        trace.append({"dropped": sorted(dropped), "where": where, "count": count})

        if count > 0:
            return result, trace
        if not has_region_identity:
            # 처음부터 지역 식별 조건이 없던 순수 자유서술 질의 -> 더 풀 것도 없이 종료
            return result, trace
        if step < len(RELAX_ORDER):
            dropped.add(RELAX_ORDER[step])

    return result, trace
