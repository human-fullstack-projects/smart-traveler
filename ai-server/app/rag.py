import os
import chromadb
from langchain_openai import OpenAIEmbeddings
from .schemas import PlaceRow

client = chromadb.PersistentClient(path="./chroma_data")
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


def index_places(rows: list[PlaceRow]) -> int:
    """장소를 임베딩해 Chroma에 upsert한다. 같은 place_id는 덮어쓴다."""
    if not rows:
        return 0
    docs = [to_document(r) for r in rows]
    places.upsert(
        ids=[str(r.placeId) for r in rows],
        embeddings=get_embeddings().embed_documents(docs),
        documents=docs,
        metadatas=[{
            "place_id": r.placeId,
            "place_type": r.placeType,
            "is_market": bool(r.isMarket),
            "sigungu_code": r.sigunguCode,
            "lat": float(r.lat),
            "lng": float(r.lng),
            "popularity": popularity(r),
        } for r in rows],
    )
    return len(rows)


def remove_places(place_ids: list[int]) -> None:
    """is_active=FALSE가 된 장소를 Chroma에서 지운다."""
    if not place_ids:
        return
    places.delete(ids=[str(i) for i in place_ids])
