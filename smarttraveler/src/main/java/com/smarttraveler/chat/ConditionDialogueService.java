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
    public ConditionDialogueService(JdbcTemplate jdbc, ObjectMapper mapper,
                                    PythonAiClient ai, StoredPlaceCandidates candidates,
                                    StoredCourseCandidates courses,
                                    @Value("${COURSE_RAG_ENABLED:false}") boolean courseRagEnabled) {
        this.jdbc = jdbc;
        this.mapper = mapper;
        this.ai = ai;
        this.candidates = candidates;
        this.courses = courses;
        this.courseRagEnabled = courseRagEnabled;
    }
    public String process(long sessionId, String message) {
        return process(sessionId, message, Map.of());
    }
    public String process(long sessionId, String message, Map<String, Object> savedPreferences) {
        return processResult(sessionId, message, savedPreferences).message();
    }
    public PythonAiClient.Result processResult(long sessionId, String message, Map<String, Object> savedPreferences) {
        List<String> saved = jdbc.query("SELECT conditions_json FROM chat_session_conditions WHERE session_id = ?", (rs, row) -> rs.getString(1), sessionId);
        ObjectNode conditions = saved.isEmpty() ? emptyConditions() : readConditions(saved.get(0));
        ObjectNode previous = conditions.deepCopy();
        List<String> last = jdbc.query("SELECT content FROM chat_messages WHERE session_id = ? AND sender_role = 'ASSISTANT' ORDER BY message_id DESC LIMIT 1", (rs, row) -> rs.getString(1), sessionId);
        String previousQuestion = last.isEmpty() ? "" : last.get(0);
        // Python alone interprets confirmation language. Supply candidates whenever
        // stored required conditions are complete, including after clarification.
        boolean mayRecommend = ConditionQuestions.CHECK.equals(new ConditionQuestions().next(conditions));
        List<Map<String, Object>> placeRows = List.of();
        List<Map<String, Object>> courseRows = null;
        if (courseRagEnabled) {
            courseRows = mayRecommend ? courses.find(conditions) : List.of();
        } else if (mayRecommend) {
            placeRows = candidates.find(conditions);
        }
        var result = ai.process(message, previousQuestion, conditions, placeRows, courseRows, savedPreferences);
        if (result.recommendedCourseId() != null && (!result.confirmed() || !conditions.equals(previous)))
            throw new IllegalStateException("변경된 조건은 재확인 후 추천해야 합니다.");
        // 내용이 달라진 경우에만 확인 상태를 해제한다.
        if (saved.isEmpty() || !conditions.equals(previous)) {
            String json = mapper.writeValueAsString(conditions);
            jdbc.update("INSERT INTO chat_session_conditions (session_id, conditions_json, confirmed) VALUES (?, ?, FALSE) ON DUPLICATE KEY UPDATE conditions_json = VALUES(conditions_json), confirmed = FALSE", sessionId, json);
        }
        if (result.confirmed() && conditions.equals(previous) && !saved.isEmpty()) {
            jdbc.update("UPDATE chat_session_conditions SET confirmed=TRUE WHERE session_id=?", sessionId);
        }
        return result;
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
