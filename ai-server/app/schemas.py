from typing import Any, Literal
from pydantic import BaseModel, ConfigDict, Field


class SavedPreferences(BaseModel):
    model_config = ConfigDict(extra="forbid")
    pace: Literal["relaxed", "normal", "packed"] | None = None
    environment: Literal["indoor", "outdoor"] | None = None
    landscapes: list[Literal["city", "countryside", "water", "island", "forest", "sea"]] = Field(default_factory=list, max_length=6)


class DialogueRequest(BaseModel):
    """스프링 서버에서 전달한 현재 메시지와 기존 조건이다."""
    model_config = ConfigDict(extra="forbid")
    message: str = Field(min_length=1, max_length=4000)
    previousQuestion: str = Field(default="", max_length=20000)
    conditions: dict[str, Any]
    candidates: list[dict[str, Any]] = Field(default_factory=list, max_length=30)
    courseCandidates: list[dict[str, Any]] | None = None
    savedPreferences: SavedPreferences | None = None
    extractOnly: bool = False  # 조건만 읽고 추천하지 않는다 (스프링이 후보를 고른 뒤 다시 부른다)
    forceRecommend: bool = False  # 조건을 다시 읽지 않고 주어진 후보로 바로 추천한다 (조건 1개여도 추천)
    showAllMatches: bool = False  # 일정·인원을 안 받았으면 취향과 무관하게 맞는 코스를 있는 만큼(최대 3개) 보여준다


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
    lodgingName: str | None = Field(description="사용자가 말한 숙소 이름")


class DialogueResponse(BaseModel):
    changes: dict[str, Any]
    question: str = Field(min_length=1, max_length=20000)
    confirmed: bool = False
    recommendedCourseId: int | None = Field(default=None, gt=0)
    recommendedCourseIds: list[int] | None = None
    recommendedPlaces: list[dict[str, int]] | None = None


class SelectedPlace(BaseModel):
    """DB 후보 중 선택한 장소의 식별자다."""
    model_config = ConfigDict(extra="forbid", strict=True)
    placeId: int


class PlaceRecommendations(BaseModel):
    model_config = ConfigDict(extra="forbid", strict=True)
    places: list[SelectedPlace]
