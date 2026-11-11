import os
from pathlib import Path
import chromadb
from langchain_openai import OpenAIEmbeddings
from . import db
from .schemas import PlaceRow

# 실행 위치와 상관없이 ai-server/chroma_data를 쓴다
CHROMA_PATH = Path(__file__).resolve().parent.parent / "chroma_data"
client = chromadb.PersistentClient(path=str(CHROMA_PATH))
places = client.get_or_create_collection("places", metadata={"hnsw:space": "cosine"})
_embeddings: OpenAIEmbeddings | None = None


def get_embeddings() -> OpenAIEmbeddings:
    """OPENAI_API_KEY가 없으면 모듈 import 시점이 아니라 실제 호출 시점에 실패하게 한다."""
    global _embeddings
    if _embeddings is None:
        if not os.getenv("OPENAI_API_KEY", "").strip():
            raise RuntimeError("OPENAI_API_KEY가 설정되지 않았습니다.")
        _embeddings = OpenAIEmbeddings(model="text-embedding-3-small")  # 문서와 질문은 같은 모델을 써야 한다
    return _embeddings


def to_document(p: PlaceRow) -> str:
    """임베딩할 텍스트. 전화번호·URL처럼 의미 없는 값은 넣지 않는다."""
    base = f"[{p.name}] {p.address or ''}\n분류: {p.placeType}\n"
    overview = (p.description or "")[:600]
    if p.placeType == "RESTAURANT":
        return base + f"대표메뉴: {p.mainMenu or ''}\n소개: {overview}"
    if p.isMarket:
        return base + f"전통시장. 휴무: {p.closedDays or ''}\n소개: {overview}"
    return base + f"소개: {overview}"


def popularity(p: PlaceRow) -> float:
    """KorService2에는 조회수가 없어 쓰는 대체 인기 신호(0~1). 중심관광지 순위가 있으면 그 값을 우선한다."""
    if p.hubRank is not None and p.hubRank > 0:
        return round(max(0.0, 1.0 - min(p.hubRank, 100) / 100), 3)
    has_image = 1.0 if p.imageUrl else 0.0
    rich_desc = min(len(p.description or "") / 500, 1.0)
    return round(0.5 * has_image + 0.5 * rich_desc, 3)


def is_indexable(p: PlaceRow) -> bool:
    """주소와 소개가 둘 다 없는 장소(이름만 있는 행)는 의미 검색에 쓸 수 없어 넣지 않는다."""
    return bool(p.description or p.address)


def to_metadata(r: PlaceRow) -> dict:
    """Chroma 메타데이터. None은 넣을 수 없으니 기본값을 채운다."""
    return {
        "place_id": r.placeId,
        "place_type": r.placeType,
        "is_market": bool(r.isMarket),
        "sigungu_code": r.sigunguCode,
        "lat": float(r.lat),
        "lng": float(r.lng),
        "popularity": popularity(r),
    }


def index_places(rows: list[PlaceRow]) -> int:
    """장소를 임베딩(OpenAI)해 Chroma에 upsert한다. 같은 place_id는 덮어쓴다."""
    rows = [r for r in rows if is_indexable(r)]
    if not rows:
        return 0
    docs = [to_document(r) for r in rows]
    places.upsert(
        ids=[str(r.placeId) for r in rows],
        embeddings=get_embeddings().embed_documents(docs),
        documents=docs,
        metadatas=[to_metadata(r) for r in rows],
    )
    return len(rows)


def remove_places(place_ids: list[int]) -> None:
    """is_active=FALSE가 된 장소를 Chroma에서 지운다."""
    if not place_ids:
        return
    places.delete(ids=[str(i) for i in place_ids])


def to_bool(v) -> bool:
    """MariaDB BIT(1) 칼럼은 pymysql이 bytes(b'\\x00'/b'\\x01')로 돌려준다.
    bool(b'\\x00')은 True이므로(빈 바이트가 아니라서) 그대로 bool()에 넘기면 안 된다."""
    if isinstance(v, bytes):
        return v != b"\x00"
    return bool(v)


def to_place_row(r: dict) -> PlaceRow:
    """MariaDB 한 행 → PlaceRow (Spring이 보내는 형식과 같다)."""
    return PlaceRow(
        placeId=r["place_id"], placeType=r["place_type"], name=r["name"], address=r["address"],
        description=r["description"], mainMenu=r["main_menu"], closedDays=r["closed_days"],
        isMarket=to_bool(r["is_market"]), sigunguCode=r["region_code"],
        lat=float(r["latitude"]), lng=float(r["longitude"]),
        imageUrl=r["image_url"], hubRank=r["hub_rank"],
    )


def sync_from_mariadb(sigungu_code: str, batch_size: int = 100) -> dict:
    """MariaDB → Chroma 적재 (한 시군구).
    1) MariaDB에서 활성·좌표 보유 장소를 읽는다
    2) 문서가 새로 생겼거나 바뀐 장소만 OpenAI로 임베딩해 넣는다 (비용 절약)
    3) 문서가 같은 장소는 메타데이터(인기도·시장 여부 등)만 고친다
    4) MariaDB에서 비활성이 됐거나 넣을 수 없게 된 장소는 Chroma에서 지운다"""
    all_rows = [to_place_row(x) for x in db.read_places(sigungu_code)]
    rows = [r for r in all_rows if is_indexable(r)]
    existing = places.get(where={"sigungu_code": sigungu_code}, include=["documents"])
    old_docs = dict(zip(existing["ids"], existing["documents"]))

    changed = [r for r in rows if old_docs.get(str(r.placeId)) != to_document(r)]
    same = [r for r in rows if old_docs.get(str(r.placeId)) == to_document(r)]
    for i in range(0, len(changed), batch_size):
        index_places(changed[i:i + batch_size])
    if same:
        places.update(ids=[str(r.placeId) for r in same], metadatas=[to_metadata(r) for r in same])

    keep = {str(r.placeId) for r in rows}
    stale = [int(i) for i in old_docs if i not in keep]
    remove_places(stale)
    return {"read": len(all_rows), "skipped": len(all_rows) - len(rows), "embedded": len(changed), "unchanged": len(same), "removed": len(stale)}
