from dotenv import load_dotenv
load_dotenv()  # .env 파일을 읽어 환경변수로 설정.

from fastapi import FastAPI, HTTPException
from . import dialogue
from .lodging_itinerary import build as build_lodging_itinerary
from .schemas import DialogueRequest, DialogueResponse, LodgingItineraryRequest, LodgingItineraryResponse

app = FastAPI(title="스마트 트래블 AI 서버")


@app.get("/health")
def health():
    """서버 실행 여부만 확인하며 OpenAI 연결 성공을 뜻하지 않는다."""
    return {"status": "ok"}


@app.post("/internal/dialogue", response_model=DialogueResponse, response_model_exclude_none=True)
def run_dialogue(request: DialogueRequest):
    """메시지 한 건을 처리한다. research/chat_test.py와 동일하게 1회성 질의이며, 대화 상태는
    저장하지 않는다 — 이전 대화 조건·확인 흐름은 스프링이 더 이상 요청하지 않는다."""
    try:
        message, course_id, course_ids = dialogue.handle(request.message, request.excludedCourseIds)
        return DialogueResponse(message=message, recommendedCourseId=course_id, recommendedCourseIds=course_ids)
    except Exception as exc:
        # 외부 오류에는 API 키나 대화 원문을 포함하지 않는다.
        raise HTTPException(status_code=503, detail="AI 요청을 처리할 수 없습니다. 서버 설정과 연결을 확인해 주세요.") from exc


@app.post("/internal/lodging-itinerary", response_model=LodgingItineraryResponse, response_model_exclude_none=True)
def lodging_itinerary(request: LodgingItineraryRequest):
    """숙소 기반 일정 추천. /internal/dialogue와 분리된 독립 API다(설계 결정 3)."""
    try:
        message, places = build_lodging_itinerary(request.model_dump())
        return LodgingItineraryResponse(message=message, recommendedPlaces=places)
    except Exception as exc:
        raise HTTPException(status_code=503, detail="AI 요청을 처리할 수 없습니다. 서버 설정과 연결을 확인해 주세요.") from exc
