package com.smarttraveler.chat;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.node.ObjectNode;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.*;

/** AI 연결 전, 일부 명확한 표현만 알아보는 임시 예시 규칙이다. */
@Component
public class RuleBasedConditionExtractor implements ConditionExtractor {
    private final JdbcTemplate jdbc;
    public RuleBasedConditionExtractor(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    private record Region(long id, String name) {}
    private static Matcher match(String pattern, String text) { return Pattern.compile(pattern).matcher(text); }
    private static void number(ObjectNode values, String key, Matcher found, int min, int max) {
        if (!found.find()) return;
        try {
            int value = Integer.parseInt(found.group(1));
            if (value >= min && value <= max) values.put(key, value);
        } catch (NumberFormatException ignored) { /* 범위를 벗어난 숫자는 추측하지 않는다. */ }
    }
    @Override
    public void extract(String message, String previousQuestion, ObjectNode values) {
        String text = message.trim();
        // 지역명은 지원 지역에서 정확히 하나만 일치할 때만 ID로 바꾼다.
        List<Region> regions = jdbc.query("SELECT region_id, region_name FROM region WHERE is_supported = 1 AND abolished_at IS NULL", (rs, row) -> new Region(rs.getLong(1), rs.getString(2)));
        Set<Long> matches = new HashSet<>();
        for (Region region : regions) {
            if (region.name() == null || region.name().length() < 2) continue;
            String pattern = "(?<![가-힣])" + Pattern.quote(region.name()) + "(?=으로|에서|에는|으로|에|을|를|와|과|[^가-힣]|$)";
            if (Pattern.compile(pattern).matcher(text).find()) matches.add(region.id());
        }
        if (matches.size() == 1) values.put("regionId", matches.iterator().next());
        Matcher regionNumber = match("지역\\s*번호\\s*(\\d+)", text);
        if (!regionNumber.find() && ConditionQuestions.REGION.equals(previousQuestion)) regionNumber = match("^\\s*(\\d+)\\s*(?:번)?\\s*$", text);
        else regionNumber.reset();
        if (regionNumber.find()) {
            try {
                long id = Long.parseLong(regionNumber.group(1));
                Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM region WHERE region_id = ? AND is_supported = 1 AND abolished_at IS NULL", Integer.class, id);
                if (count != null && count == 1) values.put("regionId", id);
            } catch (NumberFormatException ignored) { /* 올바르지 않은 번호는 반영하지 않는다. */ }
        }
        Matcher date = match("(?<!\\d)(\\d{4}-\\d{2}-\\d{2})(?!\\d)", text);
        if (date.find() && (ConditionQuestions.DATE.equals(previousQuestion) || text.contains("출발") || text.equals(date.group(1)))) {
            try { values.put("startDate", LocalDate.parse(date.group(1)).toString()); }
            catch (DateTimeParseException ignored) { /* 존재하지 않는 날짜는 반영하지 않는다. */ }
        }
        Matcher nights = match("(?<!\\d)(\\d+)\\s*박\\s*(\\d+)\\s*일", text);
        if (nights.find()) {
            try {
                int n = Integer.parseInt(nights.group(1)), days = Integer.parseInt(nights.group(2));
                if (days == n + 1 && days >= 1 && days <= 7) values.put("durationDays", days);
            } catch (NumberFormatException ignored) { /* 지나치게 큰 숫자는 무시한다. */ }
        } else if (text.contains("당일치기") || text.equals("당일")) values.put("durationDays", 1);
        else number(values, "durationDays", match("(?<![\\d-])(\\d+)\\s*일(?:간|동안|\\s*여행|\\s*$)", text), 1, 7);
        number(values, "adults", match("성인\\s*(\\d+)\\s*명?", text), 1, 100);
        number(values, "children", match("(?:아동|어린이|아이)\\s*(\\d+)\\s*명?", text), 0, 100);
        if (ConditionQuestions.ADULTS.equals(previousQuestion)) number(values, "adults", match("^\\s*(\\d+)\\s*명?\\s*$", text), 1, 100);
        if (ConditionQuestions.CHILDREN.equals(previousQuestion)) {
            number(values, "children", match("^\\s*(\\d+)\\s*명?\\s*$", text), 0, 100);
            if (text.matches("없(?:어|어요|습니다)?[.!]?")) values.put("children", 0);
        }
        if (ConditionQuestions.DURATION.equals(previousQuestion)) number(values, "durationDays", match("^\\s*(\\d+)\\s*일?\\s*$", text), 1, 7);
        Map<String, String> companions = Map.of("혼자", "SOLO", "연인", "COUPLE", "친구", "FRIENDS", "가족", "FAMILY");
        setOne(text, values, "companionType", companions);
        Map<String, String> transport = Map.of("대중교통", "PUBLIC_TRANSPORT", "자동차", "CAR", "자차", "CAR");
        setOne(text, values, "transportType", transport);
    }
    private static void setOne(String text, ObjectNode values, String field, Map<String, String> words) {
        Set<String> found = new HashSet<>();
        words.forEach((word, code) -> { if (text.contains(word)) found.add(code); });
        if (found.size() == 1) values.put(field, found.iterator().next());
    }
}
