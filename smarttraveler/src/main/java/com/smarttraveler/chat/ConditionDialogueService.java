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
    private final StoredLodgingCandidates lodgings;
    private final boolean courseRagEnabled;
    private final ChatMessageBuffer buffer;
    public ConditionDialogueService(JdbcTemplate jdbc, ObjectMapper mapper, ChatMessageBuffer buffer,
                                    PythonAiClient ai, StoredPlaceCandidates candidates,
                                    StoredCourseCandidates courses, StoredLodgingCandidates lodgings,
                                    @Value("${COURSE_RAG_ENABLED:true}") boolean courseRagEnabled) {
        this.jdbc = jdbc;
        this.mapper = mapper;
        this.ai = ai;
        this.candidates = candidates;
        this.courses = courses;
        this.lodgings = lodgings;
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
        return processResult(sessionId, message, savedPreferences, null);
    }

    public PythonAiClient.Result processResult(long sessionId, String message, Map<String, Object> savedPreferences, Long lodgingId) {
        List<String> saved = jdbc.query("SELECT conditions_json FROM chat_session_conditions WHERE session_id = ?", (rs, row) -> rs.getString(1), sessionId);
        ObjectNode conditions = saved.isEmpty() ? emptyConditions() : readConditions(saved.get(0));
        ObjectNode previous = conditions.deepCopy();
        // 저장 전 메시지까지 포함한 챗봇의 마지막 질문
        String previousQuestion = buffer.lastAssistant(sessionId);
        Map<String, Object> lodging = null;
        PythonAiClient.Result read = null;
        if (lodgingId != null) {
            lodging = lodgings.findById(lodgingId);
            if (lodging == null) return new PythonAiClient.Result("예약 화면에서 이용 가능한 숙소를 다시 선택해 주세요.", false);
            conditions.put("lodgingId", lodgingId);
            conditions.put("lodgingName", (String) lodging.get("name"));
            conditions.put("regionId", (Long) lodging.get("regionId"));
            conditions.put("regionName", (String) lodging.get("regionName"));
        } else {
            read = ai.process(message, previousQuestion, conditions, List.of(), null, savedPreferences, PythonAiClient.Mode.EXTRACT_ONLY);
        }
        if (saved.isEmpty() || !conditions.equals(previous)) {
            jdbc.update("INSERT INTO chat_session_conditions (session_id, conditions_json, confirmed) VALUES (?, ?, FALSE) ON DUPLICATE KEY UPDATE conditions_json = VALUES(conditions_json), confirmed = FALSE", sessionId, mapper.writeValueAsString(conditions));
        }
        if (!hasAnyCondition(conditions)) {
            return NO_IDEA.matcher(message).find() ? new PythonAiClient.Result(humanCourseGuide(), false) : read;
        }
        Integer duration = conditions.path("durationDays").isIntegralNumber() ? conditions.path("durationDays").asInt() : null;
        if (duration != null && duration > 4) return new PythonAiClient.Result("현재 준비된 코스는 1~4일 일정이에요. 여행 일수를 4일 이하로 알려 주세요.", false);
        if (!conditions.path("lodgingName").asText("").isBlank() && !(conditions.path("lodgingId").isIntegralNumber() && conditions.path("lodgingId").asLong() > 0)) {
            String lodgingName = conditions.path("lodgingName").asText();
            var found = lodgings.findByName(lodgingName);
            if (found.size() == 1) {
                conditions.put("lodgingId", (Long) found.get(0).get("lodgingId"));
                jdbc.update("UPDATE chat_session_conditions SET conditions_json=? WHERE session_id=?", mapper.writeValueAsString(conditions), sessionId);
            } else if (found.isEmpty()) {
                return new PythonAiClient.Result("‘" + lodgingName + "’ 숙소를 찾지 못했어요. 숙소 이름이나 지역을 다시 알려 주세요.", false);
            } else {
                StringBuilder text = new StringBuilder("같은 이름의 숙소가 여러 곳 있어요. 이름과 주소로 정확히 알려 주세요.\n");
                for (var row : found) text.append("• ").append(row.get("name")).append(" — ").append(row.get("address")).append("\n");
                return new PythonAiClient.Result(text.toString(), false);
            }
        }
        ObjectNode filled = conditions.deepCopy();
        boolean lodgingBased = filled.path("lodgingId").asLong() > 0;
        if (lodgingBased) {
            if (lodging == null) lodging = lodgings.findById(filled.path("lodgingId").asLong());
            if (lodging == null) return new PythonAiClient.Result("예약 화면에서 이용 가능한 숙소를 다시 선택해 주세요.", false);
            Long namedRegion = lodgingId != null || filled.path("regionName").asText("").isBlank()
                ? null : candidates.regionId(filled);
            if (namedRegion != null && !namedRegion.equals(lodging.get("regionId")))
                return new PythonAiClient.Result("말씀하신 지역과 숙소 위치가 달라요. 어느 지역을 기준으로 할까요?", false);
            filled.put("regionId", (Long) lodging.get("regionId"));
        }
        // 일정·인원을 둘 다 안 받았으면, 조건에 맞는 코스를 취향과 무관하게 있는 만큼(최대 3개) 보여준다.
        boolean headcountGiven = filled.path("adults").isIntegralNumber() || !filled.path("companionType").asText("").isBlank();
        boolean showAllMatches = !filled.path("durationDays").isIntegralNumber() && !headcountGiven;
        String note = fillDefaults(filled);
        if (!lodgingBased && !filled.path("regionName").asText("").isBlank() && candidates.regionId(filled) == null)
            return new PythonAiClient.Result("‘" + filled.path("regionName").asText() + "’ 지역을 찾지 못했어요. 시·도 이름(예: 부산, 제주, 강원)으로 다시 알려 주세요.", false);
        java.util.Set<Long> already = recommendedBefore(sessionId);
        boolean searchCourses = courseRagEnabled && (!lodgingBased || filled.path("regionId").asLong() > 0);
        List<Map<String, Object>> courseRows = searchCourses ? courses.find(filled, already) : List.of();
        boolean repeated = false;
        if (searchCourses && courseRows.isEmpty() && !already.isEmpty()) {
            courseRows = courses.find(filled, java.util.Set.of());
            repeated = !courseRows.isEmpty();
        }
        List<Map<String, Object>> placeRows = List.of();
        if (courseRows.isEmpty()) {
            courseRows = null;
            if (lodgingBased) {
                placeRows = places(lodging, filled);
                if (placeRows.size() < 3)
                    return new PythonAiClient.Result("주변 후보가 부족해요. 반경을 넓히거나 다른 조건으로 다시 시도해 주세요.", false);
            } else {
                placeRows = candidates.find(filled);
                if (placeRows.isEmpty())
                    return new PythonAiClient.Result("말씀하신 조건에 맞는 코스를 찾지 못했어요. 지역이나 여행 기간을 바꿔서 알려 주세요.", false);
            }
        }
        var result = ai.process(message, previousQuestion, filled.deepCopy(), placeRows, courseRows, Map.of(), PythonAiClient.Mode.FORCE_RECOMMEND, showAllMatches);
        jdbc.update("UPDATE chat_session_conditions SET confirmed=TRUE WHERE session_id=?", sessionId);
        String head = (repeated ? "새로 추천할 코스가 더 없어서 앞서 추천한 코스 중에서 골랐어요.\n" : "") + note;
        String tail = "\n\n높은 품질의 답변을 원하시면 추가 조건을 입력해 주세요." + missingConditionExamples(conditions);
        return result.withMessage((head.isEmpty() ? result.message() : head + "\n\n" + result.message()) + tail, filled);
    }

    /** 숙소 좌표 주변의 활성 관광지 (기존 코스가 없을 때만 쓴다). 취향 단어가 이름·주소에 있으면 앞으로 보낸다. */
    private List<Map<String, Object>> places(Map<String, Object> lodging, ObjectNode filled) {
        if (lodging.get("latitude") == null || lodging.get("longitude") == null) return List.of();
        var rows = candidates.findNearLodging((java.math.BigDecimal) lodging.get("latitude"), (java.math.BigDecimal) lodging.get("longitude"));
        List<String> preferences = new java.util.ArrayList<>();
        filled.path("preferences").forEach(p -> preferences.add(p.asText("")));
        if (preferences.isEmpty()) return rows;
        List<Map<String, Object>> matched = new java.util.ArrayList<>(), rest = new java.util.ArrayList<>();
        for (var row : rows) {
            String text = row.get("name") + " " + row.get("address");
            boolean hit = preferences.stream().anyMatch(p -> !p.isBlank() && text.contains(p));
            (hit ? matched : rest).add(row);
        }
        matched.addAll(rest);
        return matched;
    }

    /** 지역·날짜·기간·인원·동행·교통·취향 중 하나라도 있으면 true. */
    static boolean hasAnyCondition(JsonNode c) {
        if (c.path("regionId").isIntegralNumber() && c.path("regionId").asLong() > 0) return true;
        if (!c.path("regionName").asText("").isBlank()) return true;
        if (c.path("lodgingId").isIntegralNumber() && c.path("lodgingId").asLong() > 0) return true;
        if (!c.path("lodgingName").asText("").isBlank()) return true;
        if (!c.path("startDate").asText("").isBlank()) return true;
        for (String name : List.of("durationDays", "adults", "children")) if (c.path(name).isIntegralNumber()) return true;
        if (!c.path("companionType").asText("").isBlank() || !c.path("transportType").asText("").isBlank()) return true;
        return c.path("preferences").isArray() && !c.path("preferences").isEmpty();
    }

    /** 말하지 않은 값을 추천·저장용 기본값으로 채우고, 받은 조건과 받지 않은 조건을 함께 안내하는 문구를 돌려준다. */
    static String fillDefaults(ObjectNode c) {
        String companion = c.path("companionType").asText("");
        boolean adultsGiven = c.path("adults").isIntegralNumber();
        // 인원수는 숫자로 말하지 않아도 "친구랑", "~랑 같이" 처럼 동행이 있으면 받은 것으로 본다.
        boolean headcountGiven = adultsGiven || !companion.isEmpty();

        List<String> received = new java.util.ArrayList<>();
        if (!c.path("regionName").asText("").isBlank() || (c.path("regionId").isIntegralNumber() && c.path("regionId").asLong() > 0)) received.add("지역");
        if (!c.path("startDate").asText("").isBlank()) received.add("출발일");
        if (c.path("durationDays").isIntegralNumber()) received.add("기간");
        if (!companion.isEmpty()) received.add("동행");
        if (headcountGiven) received.add("인원수");
        if (!c.path("transportType").asText("").isEmpty()) received.add("교통");
        if (c.path("preferences").isArray() && !c.path("preferences").isEmpty()) received.add("취향");
        if (!c.path("lodgingName").asText("").isBlank()) received.add("숙소");

        List<String> parts = new java.util.ArrayList<>();
        if (!c.path("durationDays").isIntegralNumber()) { c.put("durationDays", 2); parts.add("기간 1박 2일"); }
        if (!adultsGiven) {
            c.put("adults", 1);
            if (!headcountGiven) parts.add("어른 1명");
        }
        if (!c.path("children").isIntegralNumber()) c.put("children", 0);

        List<String> any = new java.util.ArrayList<>();
        if (c.path("regionName").asText("").isBlank() && !(c.path("regionId").isIntegralNumber() && c.path("regionId").asLong() > 0)) any.add("지역");
        if (companion.isEmpty()) any.add("동행");
        if (c.path("transportType").asText("").isEmpty()) any.add("교통");

        StringBuilder text = new StringBuilder();
        if (!received.isEmpty()) text.append("입력하신 조건: ").append(String.join(", ", received));
        if (parts.isEmpty() && any.isEmpty()) return text.toString();
        if (!text.isEmpty()) text.append("\n");
        text.append("말씀하지 않은 조건은 ");
        if (!parts.isEmpty()) text.append(String.join(" · ", parts)).append("으로 잡았어요");
        if (!any.isEmpty()) text.append(parts.isEmpty() ? "" : ", ").append(String.join("·", any)).append("은 상관없음으로 골랐어요");
        return text.append(". 바꾸고 싶으면 말씀하시거나 오른쪽에서 바꿀 수 있어요.").toString();
    }

    /** 아직 입력하지 않은 조건의 예시를 줄 단위로 안내한다 (입력한 조건은 예시에서 뺀다). */
    static String missingConditionExamples(JsonNode c) {
        List<String> lines = new java.util.ArrayList<>();
        boolean regionGiven = !c.path("regionName").asText("").isBlank() || (c.path("regionId").isIntegralNumber() && c.path("regionId").asLong() > 0);
        if (!regionGiven) lines.add("여행 지역 정보 (예: 제주도, 부산, 강릉, 경주)");
        if (c.path("startDate").asText("").isBlank()) lines.add("출발 날짜 정보 (예: 12월 5일, 12월 19일, 1월 9일, 2월 6일)");
        if (!c.path("durationDays").isIntegralNumber()) lines.add("여행 기간 정보 (예: 2박 3일, 당일치기, 1박 2일, 3박 4일)");
        if (c.path("companionType").asText("").isBlank()) lines.add("동행 유형 정보 (예: 친구, 혼자, 연인, 가족)");
        if (!c.path("adults").isIntegralNumber()) lines.add("동행 인원수 정보 (예: 총 2명, 총 1명, 총 3명, 총 4명)");
        if (c.path("transportType").asText("").isBlank()) lines.add("교통 수단 정보 (예: 렌터카, 자가용, 대중교통, 도보)");
        return lines.isEmpty() ? "" : "\n" + String.join("\n", lines);
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
        node.put("lodgingName", ""); node.putNull("lodgingId");
        return node;
    }
}
