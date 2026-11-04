package com.smarttraveler.chat;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** 다른 서버의 결과를 그대로 신뢰하지 않고 허용한 조건만 합친다. */
final class ConditionMerger {
    private ConditionMerger() {}
    static void merge(JsonNode from, ObjectNode to) {
        JsonNode region = from.path("regionName");
        if (region.isTextual() && !region.asText().isBlank() && region.asText().trim().length() <= 100) {
            String name = region.asText().trim();
            if (!name.equals(to.path("regionName").asText(""))) to.putNull("regionId");
            to.put("regionName", name);
        }
        JsonNode date = from.path("startDate");
        if (date.isTextual()) {
            try { to.put("startDate", LocalDate.parse(date.asText()).toString()); }
            catch (DateTimeParseException ignored) { /* 올바르지 않은 날짜는 반영하지 않는다. */ }
        }
        number(from, to, "durationDays", 1, 7); number(from, to, "adults", 1, 100); number(from, to, "children", 0, 100);
        choice(from, to, "companionType", Set.of("SOLO", "COUPLE", "FRIENDS", "FAMILY"));
        choice(from, to, "transportType", Set.of("PUBLIC_TRANSPORT", "CAR"));
        JsonNode preferences = from.path("preferences");
        if (preferences.isArray() && preferences.size() <= 20) {
            List<String> words = new ArrayList<>(); boolean valid = true;
            for (JsonNode item : preferences) {
                if (!item.isTextual() || item.asText().isBlank() || item.asText().trim().length() > 100) { valid = false; break; }
                words.add(item.asText().trim());
            }
            if (valid) { var array = to.putArray("preferences"); words.forEach(array::add); }
        }
    }
    private static void number(JsonNode from, ObjectNode to, String field, int min, int max) {
        JsonNode value = from.path(field);
        if (value.isIntegralNumber() && value.canConvertToInt() && value.asInt() >= min && value.asInt() <= max) to.put(field, value.asInt());
    }
    private static void choice(JsonNode from, ObjectNode to, String field, Set<String> options) {
        JsonNode value = from.path(field);
        if (value.isTextual() && options.contains(value.asText())) to.put(field, value.asText());
    }
}
