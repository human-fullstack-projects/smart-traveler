package com.smarttraveler.chat;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;
import java.util.List;

/** 기존 조건을 보존하면서 새 메시지에서 확인한 값과 다음 질문을 만든다. */
@Service
public class ConditionDialogueService {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final PythonAiClient ai;
    private final StoredPlaceCandidates candidates;
    public ConditionDialogueService(JdbcTemplate jdbc, ObjectMapper mapper, PythonAiClient ai, StoredPlaceCandidates candidates) {
        this.jdbc = jdbc; this.mapper = mapper; this.ai = ai; this.candidates = candidates;
    }
    public String process(long sessionId, String message) {
        List<String> saved = jdbc.query("SELECT conditions_json FROM chat_session_conditions WHERE session_id = ?", (rs, row) -> rs.getString(1), sessionId);
        ObjectNode conditions = saved.isEmpty() ? emptyConditions() : readConditions(saved.get(0));
        ObjectNode previous = conditions.deepCopy();
        List<String> last = jdbc.query("SELECT content FROM chat_messages WHERE session_id = ? AND sender_role = 'ASSISTANT' ORDER BY message_id DESC LIMIT 1", (rs, row) -> rs.getString(1), sessionId);
        String previousQuestion = last.isEmpty() ? "" : last.get(0);
        // 조건 수집 대화에서는 관광지 조회를 실행하지 않는다.
        boolean mayRecommend = message.contains("추천") || previousQuestion.startsWith("입력된 여행 조건을 확인");
        var result = ai.process(message, previousQuestion, conditions, mayRecommend ? candidates.find(conditions) : List.of());
        // 내용이 달라진 경우에만 확인 상태를 해제한다.
        if (saved.isEmpty() || !conditions.equals(previous)) {
            String json = mapper.writeValueAsString(conditions);
            jdbc.update("INSERT INTO chat_session_conditions (session_id, conditions_json, confirmed) VALUES (?, ?, FALSE) ON DUPLICATE KEY UPDATE conditions_json = VALUES(conditions_json), confirmed = FALSE", sessionId, json);
        }
        if (result.confirmed() && conditions.equals(previous) && !saved.isEmpty()) {
            jdbc.update("UPDATE chat_session_conditions SET confirmed=TRUE WHERE session_id=?", sessionId);
        }
        return result.message();
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
