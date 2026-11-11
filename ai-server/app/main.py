import pathlib
from dotenv import load_dotenv
load_dotenv(pathlib.Path(__file__).resolve().parent.parent / ".env")  # 실행 위치와 무관하게 ai-server/.env를 읽는다

from fastapi import FastAPI, HTTPException, Query
from .tour_api import TourApiClient, TourApiError
from .graph import build_graph
from .rag import index_places, remove_places, sync_from_mariadb
from .search import search_candidates
from .course_graph import build_course_graph
from .schemas import (DialogueRequest, DialogueResponse, PlaceIndexRequest, PlaceRemoveRequest,
                      PlaceSyncRequest, PlaceSearchRequest, CourseRecommendRequest)

app = FastAPI(title="스마트 트래블 AI 서버")
graph = build_graph()                 # 기존 대화 그래프
course_graph = build_course_graph()   # 새 코스 그래프
tour = TourApiClient()


@app.get("/health")
def health():
    """서버 실행 여부만 확인하며 OpenAI 연결 성공을 뜻하지 않는다."""
    return {"status": "ok"}


@app.post("/internal/dialogue", response_model=DialogueResponse)
def dialogue(request: DialogueRequest):
    """대화 한 차례를 처리한다. 대화와 조건 저장은 스프링 서버가 맡는다."""
    try:
        result = graph.invoke(request.model_dump())
        return DialogueResponse(changes=result["changes"], question=result["question"], confirmed=result.get("confirmed", False))
    except Exception as exc:
        # 외부 오류에는 API 키나 대화 원문을 포함하지 않는다.
        raise HTTPException(status_code=503, detail="AI 요청을 처리할 수 없습니다. 서버 설정과 연결을 확인해 주세요.") from exc


@app.get("/internal/tour/regions")
def tour_regions(province: str | None = Query(default=None, pattern=r"^[0-9]{1,5}$")):
    """관광 API의 지역 목록을 조회한다."""
    try:
        return {"items": tour.regions(province)}
    except TourApiError as exc:
        raise HTTPException(status_code=503, detail=str(exc)) from None


@app.get("/internal/tour/places")
def tour_places(province: str = Query(pattern=r"^[0-9]{1,5}$"), district: str | None = Query(default=None, pattern=r"^[0-9]{1,5}$"), page: int = Query(default=1, ge=1, le=100)):
    """선택한 지역의 관광지를 조회한다."""
    try:
        return tour.places(province, district, page)
    except TourApiError as exc:
        raise HTTPException(status_code=503, detail=str(exc)) from None


@app.post("/internal/places/index")
def place_index(request: PlaceIndexRequest):
    """스프링이 동기화 뒤 보낸 장소를 임베딩해 Chroma에 저장한다. 최대 100개씩."""
    try:
        return {"indexed": index_places(request.rows)}
    except Exception as exc:
        raise HTTPException(status_code=503, detail="임베딩 서버 설정과 연결을 확인해 주세요.") from exc


@app.post("/internal/places/remove")
def place_remove(request: PlaceRemoveRequest):
    """is_active=FALSE가 된 장소를 Chroma에서 지운다."""
    remove_places(request.placeIds)
    return {"removed": len(request.placeIds)}


@app.post("/internal/places/sync")
def place_sync(request: PlaceSyncRequest):
    """MariaDB → Chroma 적재. 한 시군구의 장소를 MariaDB에서 읽어 임베딩해 넣는다."""
    try:
        return sync_from_mariadb(request.sigunguCode)
    except Exception as exc:
        raise HTTPException(status_code=503, detail="MariaDB 연결 또는 임베딩(OpenAI) 설정을 확인해 주세요.") from exc


@app.post("/internal/places/search")
def place_search(request: PlaceSearchRequest):
    """앵커 1곳과 앵커 반경 안의 슬롯별 후보를 돌려준다."""
    try:
        return search_candidates(request.sigunguCode, request.anchorName, request.transportType,
                                 request.slots, request.queries, request.excludePlaceIds)
    except Exception as exc:
        raise HTTPException(status_code=503, detail="장소 검색을 처리할 수 없습니다. 임베딩(OpenAI)과 Chroma 상태를 확인해 주세요.") from exc


@app.post("/internal/courses/recommend")
def course_recommend(request: CourseRecommendRequest):
    """LangGraph 코스 그래프를 실행한다: Chroma 검색(RAG) → 조합 → OpenAI 응답 생성 → 검증."""
    try:
        result = course_graph.invoke({"request": request.model_dump()})["result"]
    except Exception as exc:
        raise HTTPException(status_code=503, detail="코스를 만들 수 없습니다. 임베딩(OpenAI)과 Chroma 상태를 확인해 주세요.") from exc
    if "error" in result:
        raise HTTPException(status_code=422, detail=result["error"])
    return result
