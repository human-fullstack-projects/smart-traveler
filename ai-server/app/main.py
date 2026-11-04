from fastapi import FastAPI, HTTPException, Query
from .tour_api import TourApiClient, TourApiError
from .graph import build_graph
from .schemas import DialogueRequest, DialogueResponse

app = FastAPI(title="스마트 트래블 AI 서버")
graph = build_graph()
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
