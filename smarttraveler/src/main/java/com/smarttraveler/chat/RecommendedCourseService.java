package com.smarttraveler.chat;

import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

@Service
public class RecommendedCourseService {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final StoredCourseCandidates candidates;

    public RecommendedCourseService(JdbcTemplate jdbc, ObjectMapper mapper, StoredCourseCandidates candidates) {
        this.jdbc = jdbc; this.mapper = mapper; this.candidates = candidates;
    }

    /** 추천 코스가 메시지로 출력되는 즉시, 로그인 상태라면 "내 코스"에도 바로 저장한다. */
    public void record(long sessionId, long messageId, long sourceId) {
        String json = jdbc.queryForObject("SELECT conditions_json FROM chat_session_conditions WHERE session_id=?", String.class, sessionId);
        ObjectNode conditions = (ObjectNode) mapper.readTree(json);
        var rows = candidates.find(conditions).stream()
            .filter(row -> ((Number) row.get("courseId")).longValue() == sourceId).toList();
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.CONFLICT, "추천 코스를 다시 확인해 주세요.");
        var info = rows.getFirst();
        ObjectNode snapshot = mapper.createObjectNode();
        snapshot.put("sourceCourseId", sourceId);
        snapshot.put("title", info.get("title").toString());
        snapshot.put("destination", info.get("destination").toString());
        snapshot.set("regionId", mapper.valueToTree(info.get("regionId")));
        snapshot.put("generationMethodId", jdbc.queryForObject("SELECT generation_method_id FROM course WHERE id=?", Integer.class, sourceId));
        snapshot.set("conditions", conditions);
        var itinerary = snapshot.putArray("itinerary");
        int duration = conditions.path("durationDays").asInt();
        for (int day = 1; day <= duration; day++) {
            int dayNo = day;
            var visits = rows.stream().filter(row -> ((Number) row.get("dayNo")).intValue() == dayNo).toList();
            if (visits.isEmpty()) throw new ResponseStatusException(HttpStatus.CONFLICT, "추천 일정이 누락됐습니다.");
            ObjectNode entry = itinerary.addObject();
            entry.put("day_no", day); entry.put("theme", visits.getFirst().get("dayTheme").toString());
            var places = entry.putArray("visits");
            for (int index = 0; index < visits.size(); index++) {
                var visit = visits.get(index);
                if (visit.get("placeId") == null || visit.get("seq") == null || ((Number) visit.get("seq")).intValue() != index + 1)
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "추천 장소를 다시 확인해 주세요.");
                var place = places.addObject();
                place.put("seq", index + 1); place.put("place_id", ((Number) visit.get("placeId")).longValue());
                place.put("place_name", visit.get("placeName").toString());
            }
        }
        // 이 세션의 소유자가 이미 로그인 회원이면(member_id 보유) 추천 출력과 같은 트랜잭션에서 바로 영구 저장한다.
        Long memberId = jdbc.query("SELECT member_id FROM chat_sessions WHERE session_id=?",
            (rs, row) -> rs.getObject(1, Long.class), sessionId).getFirst();
        if (memberId != null) snapshot.put("savedCourseId", persist(memberId, snapshot, messageId));
        jdbc.update("UPDATE chat_messages SET recommendation_json=? WHERE message_id=? AND session_id=?", mapper.writeValueAsString(snapshot), messageId, sessionId);
    }

    @Transactional
    public long save(long memberId, long sessionId, long messageId) {
        var rows = jdbc.query("""
            SELECT m.recommendation_json FROM chat_messages m JOIN chat_sessions s ON s.session_id=m.session_id
            WHERE m.message_id=? AND m.session_id=? AND s.member_id=? AND m.sender_role='ASSISTANT'
              AND m.recommendation_json IS NOT NULL FOR UPDATE
            """, (rs, row) -> rs.getString(1), messageId, sessionId, memberId);
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "저장할 추천 코스가 없습니다.");
        ObjectNode snapshot = (ObjectNode) mapper.readTree(rows.getFirst());
        if (snapshot.hasNonNull("savedCourseId")) return snapshot.path("savedCourseId").asLong();
        long courseId = persist(memberId, snapshot, messageId);
        snapshot.put("savedCourseId", courseId);
        jdbc.update("UPDATE chat_messages SET recommendation_json=? WHERE message_id=?", mapper.writeValueAsString(snapshot), messageId);
        return courseId;
    }

    /** 추천 스냅샷을 회원의 course/course_day로 영속화하고 새 course id를 반환한다. */
    private long persist(long memberId, ObjectNode snapshot, long messageId) {
        JsonNode conditions = snapshot.path("conditions");
        LocalDate start = LocalDate.parse(conditions.path("startDate").asText());
        LocalDate end = start.plusDays(conditions.path("durationDays").asInt() - 1L);
        ObjectNode metadata = mapper.createObjectNode();
        metadata.put("source_course_id", snapshot.path("sourceCourseId").asLong());
        metadata.put("chat_message_id", messageId); metadata.set("itinerary", snapshot.path("itinerary"));
        Object[] values = {memberId, snapshot.path("destination").asText(), start, end,
            conditions.path("adults").asInt(), conditions.path("children").asInt(),
            mapper.writeValueAsString(conditions.path("fixedEvents").isArray() ? conditions.path("fixedEvents") : List.of()),
            mapper.writeValueAsString(conditions), snapshot.path("title").asText(), snapshot.path("generationMethodId").asInt(),
            conditions.path("companionType").asText(), conditions.path("transportType").asText(), conditions.path("flowType").asText("COURSE_FIRST"),
            mapper.writeValueAsString(metadata), snapshot.path("regionId").isNumber() ? snapshot.path("regionId").asLong() : null};
        var key = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            var statement = connection.prepareStatement("""
                INSERT INTO course(member_no,destination,start_date,end_date,adults,children,fixed_events,preferences_snapshot,
                  title,generation_method_id,status,companion_type,transport_type,flow_type,condition_state,condition_meta,region_id)
                VALUES (?,?,?,?,?,?,?,?,?,?,'done',?,?,?,'CONFIRMED',?,?)
                """, Statement.RETURN_GENERATED_KEYS);
            for (int index = 0; index < values.length; index++) statement.setObject(index + 1, values[index]);
            return statement;
        }, key);
        long courseId = Objects.requireNonNull(key.getKey()).longValue();
        for (JsonNode day : snapshot.path("itinerary")) {
            int dayNo = day.path("day_no").asInt();
            jdbc.update("INSERT INTO course_day(course_id,day_no,theme,visit_date) VALUES (?,?,?,?)", courseId, dayNo,
                day.path("theme").asText(), start.plusDays(dayNo - 1L));
        }
        return courseId;
    }

    public JsonNode read(String json) { return json == null ? null : mapper.readTree(json); }
}
