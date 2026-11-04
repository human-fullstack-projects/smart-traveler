package com.smarttraveler.recommendation.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/** 추천과 향후 수정 기능이 함께 사용할 코스 초안 형식이다. */
public record CourseDraft(
        CourseReference courseRef,
        String title,
        String summary,
        RecommendationRequest conditions,
        List<CourseDay> days,
        List<String> notes
) {
    /** 초안과 저장된 코스의 식별 방식을 구분한다. 아직 DB 매핑이 확정된 것은 아니다. */
    public record CourseReference(String type, String id, Long revision) {}

    public record CourseDay(int dayNo, LocalDate date, String theme, List<Activity> activities) {}

    /** activityKey는 원본과 변경안을 비교하기 위한 식별자다. */
    public record Activity(
            UUID activityKey,
            Long activityId,
            Long placeId,
            String placeName,
            int sequence,
            LocalTime startTime,
            LocalTime endTime,
            String reason
    ) {}
}
