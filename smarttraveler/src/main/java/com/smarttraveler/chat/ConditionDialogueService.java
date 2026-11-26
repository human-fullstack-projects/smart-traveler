package com.smarttraveler.chat;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.Map;

/** 메시지를 ai-server(/internal/dialogue)에 그대로 전달하고 추천을 받는다
 * (research/chat_test.py와 동일한 1회성 질의 — ai-server가 메시지 하나에서 지역·분위기·기간·동행·
 * 교통까지 전부 추출해 벡터DB+RDB로 추천까지 끝낸다). 지역을 한 번에 하나씩 묻고 조건을 쌓아가던
 * 기존 다단계 흐름은 ai-server가 더 이상 지원하지 않으므로 제거했다.
 *
 * 숙소 우선 흐름(lodgingId)만 예외로 다룬다: 숙소 지역에 저장 코스가 있으면 먼저 그걸 추천하고,
 * 없을 때만 /internal/lodging-itinerary(별도 API)로 넘어간다.
 *
 * chat_session_conditions 테이블과 PUT /conditions 조건 패널은 화면에서 계속 쓸 수 있도록 그대로
 * 두지만, 이 대화 흐름이 더는 조건을 자동으로 채우지 않는다(selectRegion()의 지역 빠른선택만 예외). */
@Service
public class ConditionDialogueService {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final PythonAiClient ai;
    private final StoredLodgingCandidates lodgings;
    private final ChatMessageBuffer buffer;

    public ConditionDialogueService(JdbcTemplate jdbc, ObjectMapper mapper, ChatMessageBuffer buffer,
                                    PythonAiClient ai, StoredLodgingCandidates lodgings) {
        this.jdbc = jdbc;
        this.mapper = mapper;
        this.ai = ai;
        this.lodgings = lodgings;
        this.buffer = buffer;
    }

    /** 「몰라·아무거나」처럼 조건 없이 맡기는 답 (피드백: 아무 답이 없으면 휴먼 추천 코스로 안내). */
    static final java.util.regex.Pattern NO_IDEA = java.util.regex.Pattern.compile(
        "몰라|모르겠|모름|아무거나|아무 ?데나|아무 ?곳이나|상관 ?없|글쎄|딱히|생각 ?안 ?나|정한 ?(게|거) ?없|알아서");

    /** savedPreferences는 더 이상 ai-server에 전달되지 않는다(1회성 질의에는 회원 취향을 반영할
     * 자리가 없다). ChatController 호출 시그니처를 바꾸지 않기 위해 매개변수만 남겨 둔다. */
    public PythonAiClient.Result processResult(long sessionId, String message, Map<String, Object> savedPreferences) {
        return processResult(sessionId, message, savedPreferences, null);
    }

    public PythonAiClient.Result processResult(long sessionId, String message, Map<String, Object> savedPreferences, Long lodgingId) {
        if (lodgingId != null) {
            var lodging = lodgings.findById(lodgingId);
            if (lodging == null) return new PythonAiClient.Result("예약 화면에서 이용 가능한 숙소를 다시 선택해 주세요.");
            return recommendForLodging(sessionId, lodgingId, lodging);
        }
        if (NO_IDEA.matcher(message).find()) {
            return new PythonAiClient.Result(humanCourseGuide());
        }
        return recommend(sessionId, message);
    }

    /** 이미 추천한 코스를 빼고 찾되, 그래서 하나도 안 남으면 제외 없이 다시 찾아 반복 추천한다. */
    private PythonAiClient.Result recommend(long sessionId, String message) {
        List<Long> excluded = List.copyOf(recommendedBefore(sessionId));
        var result = ai.process(message, excluded);
        if (result.recommendedCourseId() == null && !excluded.isEmpty()) {
            var retry = ai.process(message, List.of());
            if (retry.recommendedCourseId() != null) {
                return retry.withMessage(
                    "새로 추천할 코스가 더 없어서 앞서 추천한 코스 중에서 골랐어요.\n\n" + retry.message(),
                    baseConditions());
            }
        }
        return result.withMessage(result.message(), baseConditions());
    }

    /** 숙소 지역에 저장 코스가 있으면 먼저 추천하고, 없으면 숙소 주변 일정(별도 API)으로 넘어간다
     * (smart-traveler-lodging-course-guide.md 3번). 지역 조건은 메시지에 자연어로 담아 보내면
     * ai-server가 문장에서 직접 지역을 읽어 간다. */
    private PythonAiClient.Result recommendForLodging(long sessionId, long lodgingId, Map<String, Object> lodging) {
        String name = (String) lodging.get("name");
        String regionName = (String) lodging.get("regionName");
        String message = "숙소: " + name + (regionName == null || regionName.isBlank() ? "" : ", 지역: " + regionName);
        List<Long> excluded = List.copyOf(recommendedBefore(sessionId));
        var result = ai.process(message, excluded);
        if (result.recommendedCourseId() != null) {
            return result.withMessage(result.message(), baseConditions());
        }
        return ai.recommendLodgingItinerary(lodgingId, mapper.createObjectNode());
    }

    /** 코스 저장에 필요한 최소 기본값. ai-server는 더 이상 기간·인원·동행·교통을 추출해 돌려주지
     * 않으므로, RecommendedCourseService가 저장할 때 쓸 안전한 기본값을 여기서 채운다. 저장 시점의
     * 출발일은 기존처럼 SaveInput.startDate()로 별도 입력받는다(applyStartDateOverride). */
    private ObjectNode baseConditions() {
        ObjectNode node = mapper.createObjectNode();
        node.put("adults", 1);
        node.put("children", 0);
        node.put("companionType", "");
        node.put("transportType", "");
        node.put("flowType", "COURSE_FIRST");
        return node;
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

    /** 지역 빠른선택 버튼. 조건 패널(chat_session_conditions)에 지역만 채워 화면에 보여준다 —
     * 채팅으로 추천을 요청하는 흐름과는 별개다. */
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
