from typing import Any, Literal
from pydantic import BaseModel, ConfigDict, Field


class DialogueRequest(BaseModel):
    """스프링 서버에서 전달한 현재 메시지와 기존 조건이다."""
    model_config = ConfigDict(extra="forbid")
    message: str = Field(min_length=1, max_length=4000)
    previousQuestion: str = Field(default="", max_length=3000)
    conditions: dict[str, Any]
    candidates: list[dict[str, Any]] = Field(default_factory=list, max_length=30)


class ExtractedConditions(BaseModel):
    """메시지에서 명확하게 확인된 값만 반환하고 나머지는 null로 둔다."""
    model_config = ConfigDict(extra="forbid", strict=True)
    regionName: str | None = Field(description="사용자가 말한 지역 이름")
    startDate: str | None = Field(description="출발 날짜 YYYY-MM-DD")
    durationDays: int | None = Field(description="여행 일수")
    adults: int | None = Field(description="성인 인원")
    children: int | None = Field(description="아동 인원")
    companionType: Literal["SOLO", "COUPLE", "FRIENDS", "FAMILY"] | None
    transportType: Literal["PUBLIC_TRANSPORT", "CAR"] | None
    preferences: list[str] | None = Field(description="변경된 경우 갱신된 전체 여행 취향")


class DialogueResponse(BaseModel):
    changes: dict[str, Any]
    question: str
    confirmed: bool = False


class SelectedPlace(BaseModel):
    """DB 후보 중 선택한 장소의 식별자다."""
    model_config = ConfigDict(extra="forbid", strict=True)
    placeId: int


class PlaceRecommendations(BaseModel):
    model_config = ConfigDict(extra="forbid", strict=True)
    places: list[SelectedPlace]
