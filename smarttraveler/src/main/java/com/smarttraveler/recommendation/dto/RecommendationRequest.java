package com.smarttraveler.recommendation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** 사용자가 확인한 여행 조건. 날짜와 일수는 종료일을 계산하는 기준이다. */
public record RecommendationRequest(
        @NotNull @Positive Long regionId,
        @NotNull LocalDate startDate,
        @NotNull @Min(1) @Max(7) Integer durationDays,
        @NotNull @Min(1) Integer adults,
        @NotNull @Min(0) Integer children,
        @NotBlank @Size(max = 30) String companionType,
        @NotBlank @Size(max = 30) String transportType,
        @NotNull @Size(max = 20) List<@NotBlank @Size(max = 100) String> preferences,
        @NotNull @Size(max = 30) List<@NotNull @Valid FixedEvent> fixedEvents,
        @NotBlank @Pattern(regexp = "COURSE_FIRST|STAY_FIRST") String flowType,
        @Valid LodgingSelection lodgingSelection
) {
    /** 숙소 먼저 방식은 실제 선택한 숙소·객실 정보가 있어야 한다. */
    @AssertTrue(message = "숙소 먼저 방식에서는 숙소, 객실과 숙박 날짜가 필요합니다.")
    public boolean isLodgingSelectionValid() {
        return !"STAY_FIRST".equals(flowType) || lodgingSelection != null;
    }

    public record LodgingSelection(
            @NotNull @Positive Long lodgingId,
            @NotNull @Positive Long roomId,
            @NotNull LocalDate checkIn,
            @NotNull LocalDate checkOut
    ) {
        @AssertTrue(message = "체크아웃은 체크인보다 이후여야 합니다.")
        public boolean isDateRangeValid() {
            return checkIn == null || checkOut == null || checkOut.isAfter(checkIn);
        }
    }

    /** 사용자가 일정에 반드시 포함하도록 요청한 일정이다. */
    public record FixedEvent(
            @NotNull LocalDate date,
            @NotNull LocalTime startTime,
            @NotBlank @Size(max = 200) String placeName
    ) {}
}
