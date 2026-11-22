package com.smarttraveler.chat;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.Map;

/** 기존 조건을 보존하면서 새 메시지에서 확인한 값과 다음 질문을 만든다. */
@Service
public class ConditionDialogueService {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final PythonAiClient ai;
    private final StoredPlaceCandidates candidates;
    private final StoredCourseCandidates courses;
    private final boolean courseRagEnabled;
    private final ChatMessageBuffer buffer;
    public ConditionDialogueService(JdbcTemplate jdbc, ObjectMapper mapper, ChatMessageBuffer buffer,
                                    PythonAiClient ai, StoredPlaceCandidates candidates,
                                    StoredCourseCandidates courses,
                                    @Value("${COURSE_RAG_ENABLED:true}") boolean courseRagEnabled) {
        this.jdbc = jdbc;
        this.mapper = mapper;
        this.ai = ai;
        this.candidates = candidates;
        this.courses = courses;
        this.courseRagEnabled = courseRagEnabled;
        this.buffer = buffer;
    }
    public String process(long sessionId, String message) {
        return process(sessionId, message, Map.of());
    }
    public String process(long sessionId, String message, Map<String, Object> savedPreferences) {
        return processResult(sessionId, message, savedPreferences).message();
    }
    /** 「몰라·아무거나」처럼 조건 없이 맡기는 답 (피드백: 아무 답이 없으면 휴먼 추천 코스로 안내). */
    static final java.util.regex.Pattern NO_IDEA = java.util.regex.Pattern.compile(
        "몰라|모르겠|모름|아무거나|아무 ?데나|아무 ?곳이나|상관 ?없|글쎄|딱히|생각 ?안 ?나|정한 ?(게|거) ?없|알아서");

    /**
     * 한 차례 대화. ① AI 서버가 메시지에서 조건만 읽는다(EXTRACT_ONLY) ② 조건이 1개라도 있으면 말하지 않은 값은
     * 기본값으로 채워 바로 추천한다(FORCE_RECOMMEND) ③ 조건이 하나도 없고 「몰라」류 답이면 휴먼 추천 코스를 안내한다.
     */
    public PythonAiClient.Result processResult(long sessionId, String message, Map<String, Object> savedPreferences) {
        List<String> saved = jdbc.query("SELECT conditions_json FROM chat_session_conditions WHERE session_id = ?", (rs, row) -> rs.getString(1), sessionId);
        ObjectNode conditions = saved.isEmpty() ? emptyConditions() : readConditions(saved.get(0));
        ObjectNode previous = conditions.deepCopy();
        // 저장 전 메시지까지 포함한 챗봇의 마지막 질문
        String previousQuestion = buffer.lastAssistant(sessionId);
        var read = ai.process(message, previousQuestion, conditions, List.of(), null, savedPreferences, PythonAiClient.Mode.EXTRACT_ONLY);
        if (saved.isEmpty() || !conditions.equals(previous)) {
            jdbc.update("INSERT INTO chat_session_conditions (session_id, conditions_json, confirmed) VALUES (?, ?, FALSE) ON DUPLICATE KEY UPDATE conditions_json = VALUES(conditions_json), confirmed = FALSE", sessionId, mapper.writeValueAsString(conditions));
        }
        if (!hasAnyCondition(conditions)) {
            return NO_IDEA.matcher(message).find() ? new PythonAiClient.Result(humanCourseGuide(), false) : read;
        }
        int duration = conditions.path("durationDays").isIntegralNumber() ? conditions.path("durationDays").asInt() : 2;
        if (duration > 4) return new PythonAiClient.Result("현재 준비된 코스는 1~4일 일정이에요. 여행 일수를 4일 이하로 알려 주세요.", false);
        ObjectNode filled = conditions.deepCopy();
        String note = fillDefaults(filled);
        if (!filled.path("regionName").asText("").isBlank() && candidates.regionId(filled) == null)
            return new PythonAiClient.Result("‘" + filled.path("regionName").asText() + "’ 지역을 찾지 못했어요. 시·도 이름(예: 부산, 제주, 강원)으로 다시 알려 주세요.", false);
        java.util.Set<Long> already = recommendedBefore(sessionId);
        List<Map<String, Object>> courseRows = courseRagEnabled ? courses.find(filled, already) : List.of();
        boolean repeated = false;
        if (courseRagEnabled && courseRows.isEmpty() && !already.isEmpty()) {
            courseRows = courses.find(filled, java.util.Set.of());
            repeated = !courseRows.isEmpty();
        }
        List<Map<String, Object>> placeRows = List.of();
        if (courseRows.isEmpty()) {
            courseRows = null;
            placeRows = candidates.find(filled);
            if (placeRows.isEmpty())
                return new PythonAiClient.Result("말씀하신 조건에 맞는 코스를 찾지 못했어요. 지역이나 여행 기간을 바꿔서 알려 주세요.", false);
        }
        var result = ai.process(message, previousQuestion, filled.deepCopy(), placeRows, courseRows, savedPreferences, PythonAiClient.Mode.FORCE_RECOMMEND);
        jdbc.update("UPDATE chat_session_conditions SET confirmed=TRUE WHERE session_id=?", sessionId);
        String head = (repeated ? "새로 추천할 코스가 더 없어서 앞서 추천한 코스 중에서 골랐어요.\n" : "") + note;
        return result.withMessage(head.isEmpty() ? result.message() : head + "\n\n" + result.message(), filled);
    }

    /** 지역·날짜·기간·인원·동행·교통·취향 중 하나라도 있으면 true. */
    static boolean hasAnyCondition(JsonNode c) {
        if (c.path("regionId").isIntegralNumber() && c.path("regionId").asLong() > 0) return true;
        if (!c.path("regionName").asText("").isBlank()) return true;
        if (!c.path("startDate").asText("").isBlank()) return true;
        for (String name : List.of("durationDays", "adults", "children")) if (c.path(name).isIntegralNumber()) return true;
        if (!c.path("companionType").asText("").isBlank() || !c.path("transportType").asText("").isBlank()) return true;
        return c.path("preferences").isArray() && !c.path("preferences").isEmpty();
    }

    /** 말하지 않은 값을 추천·저장용 기본값으로 채우고, 사용자에게 보여 줄 안내 한 줄을 돌려준다. */
    static String fillDefaults(ObjectNode c) {
        List<String> parts = new java.util.ArrayList<>();
        if (!c.path("durationDays").isIntegralNumber()) { c.put("durationDays", 2); parts.add("기간 1박 2일"); }
        try { java.time.LocalDate.parse(c.path("startDate").asText("")); }
        catch (java.time.format.DateTimeParseException ex) {
            java.time.LocalDate start = java.time.LocalDate.now(java.time.ZoneId.of("Asia/Seoul")).plusDays(7);
            c.put("startDate", start.toString());
            parts.add("출발 " + start.getMonthValue() + "월 " + start.getDayOfMonth() + "일");
        }
        String companion = c.path("companionType").asText("");
        if (!c.path("adults").isIntegralNumber()) {
            int adults = "SOLO".equals(companion) || companion.isEmpty() ? 1 : 2;
            c.put("adults", adults); parts.add("어른 " + adults + "명");
        }
        if (!c.path("children").isIntegralNumber()) c.put("children", 0);
        List<String> any = new java.util.ArrayList<>();
        if (c.path("regionName").asText("").isBlank() && !(c.path("regionId").isIntegralNumber() && c.path("regionId").asLong() > 0)) any.add("지역");
        if (companion.isEmpty()) any.add("동행");
        if (c.path("transportType").asText("").isEmpty()) any.add("교통");
        if (parts.isEmpty() && any.isEmpty()) return "";
        StringBuilder text = new StringBuilder("말씀하지 않은 조건은 ");
        if (!parts.isEmpty()) text.append(String.join(" · ", parts)).append("으로 잡았어요");
        if (!any.isEmpty()) text.append(parts.isEmpty() ? "" : ", ").append(String.join("·", any)).append("은 상관없음으로 골랐어요");
        return text.append(". 바꾸고 싶으면 말씀하시거나 오른쪽에서 바꿀 수 있어요.").toString();
    }

    /** 이 대화에서 이미 추천한 원본 코스 번호 (같은 코스를 다시 추천하지 않기). */
    private java.util.Set<Long> recommendedBefore(long sessionId) {
        java.util.Set<Long> ids = new java.util.HashSet<>();
        for (var m : buffer.messages(sessionId)) {
            if (m.recommendationJson() == null) continue;
            long id = mapper.readTree(m.recommendationJson()).path("sourceCourseId").asLong(0);
            if (id > 0) ids.add(id);
        }
        return ids;
    }

    /** 좋아요 → 댓글 → 최신 순 휴먼 추천 코스 3개 안내 (화면이 [제목](#/...)을 링크로 바꿈). */
    private String humanCourseGuide() {
        var rows = jdbc.queryForList("""
            SELECT p.post_id, p.title
            FROM human_course_post p
            WHERE p.status='VISIBLE' AND p.deleted_at IS NULL
            ORDER BY (SELECT COUNT(*) FROM human_course_like l WHERE l.post_id=p.post_id) DESC,
                     (SELECT COUNT(*) FROM human_course_comment c WHERE c.post_id=p.post_id AND c.deleted_at IS NULL) DESC,
                     p.created_at DESC, p.post_id DESC
            LIMIT 3
            """);
        StringBuilder text = new StringBuilder("아직 정한 게 없으시군요. 다른 여행자들이 직접 다녀온 **휴먼 추천 코스**를 먼저 둘러보세요.");
        for (var row : rows) text.append("\n• [").append(String.valueOf(row.get("title")).replace("]", ")"))
            .append("](#/human-courses/").append(row.get("post_id")).append(")");
        text.append("\n• [휴먼 추천 코스 전체 보기](#/human-courses)");
        text.append("\n가고 싶은 지역이나 함께 갈 사람만 말씀해 주셔도 바로 코스를 추천해 드릴게요.");
        return text.toString();
    }
    public String selectRegion(long sessionId, CourseRegionController.Region region) {
        List<String> saved = jdbc.query("SELECT conditions_json FROM chat_session_conditions WHERE session_id = ?", (rs, row) -> rs.getString(1), sessionId);
        ObjectNode conditions = saved.isEmpty() ? emptyConditions() : readConditions(saved.get(0));
        conditions.put("regionId", region.regionId()); conditions.put("regionName", region.fullName());
        jdbc.update("INSERT INTO chat_session_conditions (session_id, conditions_json, confirmed) VALUES (?, ?, FALSE) ON DUPLICATE KEY UPDATE conditions_json=VALUES(conditions_json), confirmed=FALSE", sessionId, mapper.writeValueAsString(conditions));
        return new ConditionQuestions().next(conditions);
    }
    private ObjectNode readConditions(String json) {
        JsonNode node = mapper.readTree(json);
        if (!(node instanceof ObjectNode object)) throw new IllegalStateException("저장된 여행 조건의 형식이 올바르지 않습니다.");
        return object;
    }
    private ObjectNode emptyConditions() {
        ObjectNode node = mapper.createObjectNode();
        node.putNull("regionId"); node.put("regionName", ""); node.put("startDate", ""); node.putNull("durationDays");
        node.putNull("adults"); node.putNull("children"); node.put("companionType", ""); node.put("transportType", "");
        node.putArray("preferences"); node.putArray("fixedEvents"); node.put("flowType", "COURSE_FIRST"); node.putNull("lodgingSelection");
        return node;
    }
}
