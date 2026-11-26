"""
course 테이블 -> ChromaDB 색인 배치 프로그램
설계 문서: research/vector_db_design.md 기준. 로직은 research/build_vector_db.py와 동일하며
(결정: 주기적 전체 재색인 방식을 그대로 유지), ai-server 운영 경로/환경변수 이름만 맞췄다.

운영에서는 이 스크립트를 주기적 배치(cron 등)로 --reset과 함께 실행해 Chroma 컬렉션을 통째로 새로 만든다.
증분 upsert는 하지 않으므로, 삭제·비활성화된 코스가 다음 주기까지 잠깐 남아있을 수 있다(알려진 한계).

실행 예 (ai-server 폴더에서):
  .\.venv\Scripts\python.exe scripts\build_vector_db.py --reset
  .\.venv\Scripts\python.exe scripts\build_vector_db.py --limit 20 --dry-run
"""
import argparse
import json
import os
from collections import Counter

import chromadb
import pymysql
from openai import OpenAI

DB_HOST = os.environ.get("DB_HOST", "127.0.0.1")
DB_PORT = int(os.environ.get("DB_PORT", "3306"))
DB_USERNAME = os.environ.get("DB_USERNAME", "sixon")
DB_PASSWORD = os.environ.get("DB_PASSWORD", "123456")
DB_NAME = os.environ.get("DB_NAME", "smart_travel_db")

CHROMA_PATH = os.environ.get(
    "CHROMA_PATH",
    os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "data", "chroma_db"),
)
COLLECTION_NAME = "travel_courses"

EMBEDDING_BATCH_SIZE = 100

# 3.1 theme_code -> purpose_theme 매핑 (지역테마 성격 코드는 purpose_theme 생략)
THEME_CODE_TO_PURPOSE = {
    "HISTORY_CULTURE": "역사·전통",
    "FOOD_CAFE": "맛집",
    "TEMPLE_HEALING": "사찰·힐링",
    "NATURE_HEALING": "사찰·힐링",
}

# 지역테마 -> 메타데이터 키 접미사
REGION_THEME_SLUG = {
    "도시": "city",
    "바다": "sea",
    "산·숲": "mountain",
    "섬": "island",
    "시골": "rural",
    "호수·강": "lake_river",
    "어촌·포구": "fishing_village",
}

# 구버전 스키마(57건) 역산용 키워드 규칙 - best-effort, 정확도 낮음을 전제로 함
REGION_KEYWORDS = {
    "바다": ["바다", "해변", "해수욕장", "해안"],
    "어촌·포구": ["어촌", "포구", "수산시장"],
    "산·숲": ["등산", "산행", "수목원", "휴양림"],
    "호수·강": ["호수", "저수지", "하천"],
    "시골": ["체험마을", "농장", "농원", "농촌"],
    "섬": ["섬"],
}

PURPOSE_KEYWORDS = {
    "사찰·힐링": ["사찰", "암자", "템플스테이", "온천", "스파", "찜질방"],
    "역사·전통": ["박물관", "전시관", "유적", "고분", "읍성", "서원", "향교", "한옥", "궁", "민속"],
    "쇼핑": ["시장", "백화점", "면세점", "아울렛"],
    "맛집": ["맛집", "식당", "먹거리"],
    "문화": ["공연장", "미술관", "테마공원"],
}

# 분위기 묘사 보강 문구 (방식 A: 테마값 -> 고정 문구 매핑, summary에 덧붙여 임베딩)
REGION_MOOD_SENTENCE = {
    "도시": "활기차고 분주한 도심 분위기를 느낄 수 있습니다.",
    "바다": "시원하고 탁 트인 바다 분위기를 느낄 수 있습니다.",
    "산·숲": "맑은 공기와 푸르름이 가득한 분위기를 느낄 수 있습니다.",
    "섬": "이국적이고 평화로운 섬 분위기를 느낄 수 있습니다.",
    "시골": "소박하고 정겨운 시골 분위기를 느낄 수 있습니다.",
    "호수·강": "잔잔하고 평온한 물가 분위기를 느낄 수 있습니다.",
    "어촌·포구": "소소한 정취가 있는 포구 분위기를 느낄 수 있습니다.",
}

PURPOSE_MOOD_SENTENCE = {
    "역사·전통": "역사와 전통의 깊이를 차분히 들여다볼 수 있습니다.",
    "맛집": "현지 맛집을 즐기며 여유롭게 둘러볼 수 있습니다.",
    "사찰·힐링": "조용하고 한적하게 머리를 비우며 힐링할 수 있습니다.",
    "쇼핑": "쇼핑과 트렌디한 핫플레이스를 즐길 수 있습니다.",
    "문화": "공연과 전시 등 문화를 즐길 수 있습니다.",
}


def append_mood_sentences(document, purpose_theme, region_themes):
    sentences = [REGION_MOOD_SENTENCE[t] for t in region_themes if t in REGION_MOOD_SENTENCE]
    if purpose_theme in PURPOSE_MOOD_SENTENCE:
        sentences.append(PURPOSE_MOOD_SENTENCE[purpose_theme])
    if not sentences:
        return document
    return document + " " + " ".join(sentences)


def fetch_rows(conn, limit=None):
    sql = """
        SELECT id, destination, start_date, end_date, adults, children,
               companion_type, transport_type, preferences_snapshot,
               condition_meta, summary
        FROM course
        WHERE status = 'done'
          AND deleted_at IS NULL
          AND is_active = 1
        ORDER BY id
    """
    if limit:
        sql += f" LIMIT {int(limit)}"
    with conn.cursor(pymysql.cursors.DictCursor) as cur:
        cur.execute(sql)
        return cur.fetchall()


def dedup_by_route_hash(rows):
    seen = set()
    deduped = []
    for row in rows:
        route_hash = None
        try:
            cm = json.loads(row["condition_meta"]) if row["condition_meta"] else {}
            route_hash = cm.get("route_hash")
        except (json.JSONDecodeError, TypeError):
            pass
        key = route_hash or row["id"]
        if key in seen:
            continue
        seen.add(key)
        deduped.append(row)
    return deduped


def parse_preferences(raw):
    try:
        data = json.loads(raw) if raw else {}
    except (json.JSONDecodeError, TypeError):
        return {}
    return data if isinstance(data, dict) else {}


def itinerary_text(condition_meta_raw):
    try:
        cm = json.loads(condition_meta_raw) if condition_meta_raw else {}
    except (json.JSONDecodeError, TypeError):
        return "", []
    names = []
    for day in cm.get("itinerary", []):
        for visit in day.get("visits", []):
            names.append(visit.get("place_name", ""))
    blob = " ".join(names) + " " + " ".join(
        visit.get("address", "")
        for day in cm.get("itinerary", [])
        for visit in day.get("visits", [])
    )
    return blob, names


def infer_region_themes(text):
    matched = []
    for theme, keywords in REGION_KEYWORDS.items():
        if any(kw in text for kw in keywords):
            matched.append(theme)
    return matched


def infer_purpose_theme(text):
    best_theme, best_count = None, 0
    for theme, keywords in PURPOSE_KEYWORDS.items():
        count = sum(text.count(kw) for kw in keywords)
        if count > best_count:
            best_theme, best_count = theme, count
    return best_theme


def build_fallback_summary(destination, duration_days, place_names):
    if not place_names:
        return f"{destination}에서 즐기는 {duration_days}일 여행 코스입니다."
    joined = ", ".join(place_names)
    return f"{destination}에서 즐기는 {duration_days}일 여행 코스입니다. 방문지: {joined} 순서로 둘러봅니다."


def build_record(row, stats):
    course_id = row["id"]
    duration_days = (row["end_date"] - row["start_date"]).days + 1
    prefs = parse_preferences(row["preferences_snapshot"])
    itin_blob, place_names = itinerary_text(row["condition_meta"])

    is_legacy = "theme_code" not in prefs and "preferred_region" not in prefs
    metadata = {
        "course_id": course_id,
        "destination": row["destination"] or "",
        "duration_days": duration_days,
        "companion_type": row["companion_type"] or "",
        "adults": row["adults"] or 0,
        "children": row["children"] or 0,
        "transport_type": row["transport_type"] or "",
        "schema_version": "v1_legacy" if is_legacy else "v2",
    }

    if is_legacy:
        stats["legacy_rows"] += 1
        region_themes = infer_region_themes(row["destination"] + " " + itin_blob)
        purpose_theme = infer_purpose_theme(itin_blob)
    else:
        region_themes = prefs.get("preferred_region") or []
        theme_code = prefs.get("theme_code")
        purpose_theme = THEME_CODE_TO_PURPOSE.get(theme_code)
        metadata["travel_style"] = prefs.get("travel_style") or ""
        metadata["place_preference"] = prefs.get("place_preference") or ""

    for theme_name, slug in REGION_THEME_SLUG.items():
        metadata[f"region_theme_{slug}"] = theme_name in region_themes

    if purpose_theme:
        metadata["purpose_theme"] = purpose_theme
        stats["purpose_assigned"] += 1
    else:
        stats["purpose_missing"] += 1

    if not region_themes:
        stats["region_missing"] += 1
    stats["region_theme_counter"].update(region_themes)

    document = row["summary"] or build_fallback_summary(
        metadata["destination"], duration_days, place_names
    )
    document = append_mood_sentences(document, purpose_theme, region_themes)
    return str(course_id), document, metadata


def embed_texts(client, model, texts):
    embeddings = []
    for i in range(0, len(texts), EMBEDDING_BATCH_SIZE):
        chunk = texts[i : i + EMBEDDING_BATCH_SIZE]
        resp = client.embeddings.create(model=model, input=chunk)
        embeddings.extend(item.embedding for item in resp.data)
    return embeddings


def main():
    parser = argparse.ArgumentParser(description="course -> ChromaDB 색인 배치")
    parser.add_argument(
        "--model",
        choices=["text-embedding-3-small", "text-embedding-3-large"],
        default="text-embedding-3-small",
    )
    parser.add_argument("--limit", type=int, default=None, help="테스트용 row 수 제한")
    parser.add_argument("--dry-run", action="store_true", help="임베딩/적재 없이 통계만 출력")
    parser.add_argument("--chroma-path", default=CHROMA_PATH)
    parser.add_argument("--collection", default=COLLECTION_NAME)
    parser.add_argument("--reset", action="store_true", help="기존 컬렉션을 삭제하고 새로 생성 후 적재")
    args = parser.parse_args()

    conn = pymysql.connect(
        host=DB_HOST, port=DB_PORT, user=DB_USERNAME, password=DB_PASSWORD, database=DB_NAME, charset="utf8mb4"
    )
    try:
        rows = fetch_rows(conn, limit=args.limit)
    finally:
        conn.close()

    rows = dedup_by_route_hash(rows)

    stats = {
        "legacy_rows": 0,
        "purpose_assigned": 0,
        "purpose_missing": 0,
        "region_missing": 0,
        "region_theme_counter": Counter(),
    }

    ids, documents, metadatas = [], [], []
    for row in rows:
        doc_id, document, metadata = build_record(row, stats)
        ids.append(doc_id)
        documents.append(document)
        metadatas.append(metadata)

    print(f"총 대상 row: {len(rows)}건 (중복제거 후)")
    print(f"구버전 스키마(역산): {stats['legacy_rows']}건")
    print(f"목적테마 할당: {stats['purpose_assigned']}건 / 미할당: {stats['purpose_missing']}건")
    print(f"지역테마 미매칭: {stats['region_missing']}건")
    print(f"지역테마 분포: {dict(stats['region_theme_counter'])}")

    if args.dry_run:
        print("dry-run 모드: 임베딩/Chroma 적재는 수행하지 않았습니다.")
        return

    client = OpenAI()
    embeddings = embed_texts(client, args.model, documents)

    chroma_client = chromadb.PersistentClient(path=args.chroma_path)
    if args.reset:
        try:
            chroma_client.delete_collection(args.collection)
            print(f"기존 컬렉션 삭제: {args.collection}")
        except Exception:
            pass
    collection = chroma_client.get_or_create_collection(
        name=args.collection,
        metadata={"embedding_model": args.model},
    )
    collection.upsert(ids=ids, documents=documents, metadatas=metadatas, embeddings=embeddings)

    print(f"Chroma 적재 완료: {len(ids)}건 -> {args.chroma_path} (collection={args.collection})")


if __name__ == "__main__":
    main()
