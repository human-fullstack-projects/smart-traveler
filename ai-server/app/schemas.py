from typing import Any
from pydantic import BaseModel, ConfigDict, Field


class DialogueRequest(BaseModel):
    """스프링이 보낸 사용자 메시지 한 건. research/chat_test.py와 동일하게 이전 대화 상태를
    들고 있지 않는 1회성 질의다 — 지역·기간·동행·교통·분위기까지 전부 이 메시지 하나에서 추출한다."""
    model_config = ConfigDict(extra="forbid")
    message: str = Field(min_length=1, max_length=4000)
    excludedCourseIds: list[int] = Field(default_factory=list, max_length=100)


class DialogueResponse(BaseModel):
    message: str = Field(min_length=1, max_length=20000)
    recommendedCourseId: int | None = Field(default=None, gt=0)
    recommendedCourseIds: list[int] | None = None


class LodgingItineraryRequest(BaseModel):
    """스프링의 숙소 우선 흐름이 호출한다. 숙소 조회·주변 장소 조회는 ai-server가 직접 한다
    (/internal/dialogue와 분리된 독립 API — 설계 결정 3)."""
    model_config = ConfigDict(extra="forbid")
    lodgingId: int = Field(gt=0)
    durationDays: int | None = Field(default=None, ge=1, le=7)
    startDate: str | None = None
    preferences: list[str] = Field(default_factory=list, max_length=20)


class LodgingItineraryResponse(BaseModel):
    message: str = Field(min_length=1, max_length=20000)
    recommendedPlaces: list[dict[str, Any]] | None = None
