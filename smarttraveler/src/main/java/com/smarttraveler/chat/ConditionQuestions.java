package com.smarttraveler.chat;

import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/** 반드시 필요한 조건 중 첫 번째 빈 항목을 질문한다. */
@Component
public class ConditionQuestions {
    static final String REGION = "어느 지역으로 여행하시나요? 지역 이름을 알려주세요.";
    static final String DATE = "출발 날짜를 알려주세요. 예: 2026-12-10";
    static final String DURATION = "여행은 며칠 동안 가시나요? 예: 2박 3일";
    static final String ADULTS = "함께 여행할 성인은 몇 명인가요?";
    static final String CHILDREN = "함께 여행할 아동은 몇 명인가요? 없으면 0명이라고 알려주세요.";
    static final String COMPANION = "누구와 함께 가시나요? 혼자, 연인, 친구, 가족 중에서 알려주세요.";
    static final String TRANSPORT = "이동할 때 대중교통과 자동차 중 무엇을 이용하시나요?";
    static final String CHECK = "입력된 여행 조건을 확인해 주세요. 수정할 내용은 오른쪽에서 바꿀 수 있어요.";
    static final String STAY = "숙소 먼저 진행하려면 오른쪽에서 숙소와 객실, 숙박 날짜를 입력해 주세요.";
    public String next(JsonNode value) {
        if (!positive(value, "regionId", 1, Long.MAX_VALUE) && value.path("regionName").asText("").isBlank()) return REGION;
        try { LocalDate.parse(value.path("startDate").asText("")); }
        catch (DateTimeParseException ex) { return DATE; }
        if (!positive(value, "durationDays", 1, 7)) return DURATION;
        if (!positive(value, "adults", 1, Integer.MAX_VALUE)) return ADULTS;
        if (!positive(value, "children", 0, Integer.MAX_VALUE)) return CHILDREN;
        if (!java.util.Set.of("SOLO", "COUPLE", "FRIENDS", "FAMILY").contains(value.path("companionType").asText(""))) return COMPANION;
        if (!java.util.Set.of("CAR", "PUBLIC_TRANSPORT").contains(value.path("transportType").asText(""))) return TRANSPORT;
        if ("STAY_FIRST".equals(value.path("flowType").asText()) && !validStay(value.path("lodgingSelection"))) return STAY;
        return CHECK;
    }
    private static boolean validStay(JsonNode stay) {
        if (!positive(stay, "lodgingId", 1, Long.MAX_VALUE) || !positive(stay, "roomId", 1, Long.MAX_VALUE)) return false;
        try { return LocalDate.parse(stay.path("checkOut").asText("")).isAfter(LocalDate.parse(stay.path("checkIn").asText(""))); }
        catch (DateTimeParseException ex) { return false; }
    }
    private static boolean positive(JsonNode value, String name, long min, long max) {
        JsonNode field = value.path(name);
        return field.isIntegralNumber() && field.canConvertToLong() && field.asLong() >= min && field.asLong() <= max;
    }
}
