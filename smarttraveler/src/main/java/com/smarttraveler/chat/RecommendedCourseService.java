package com.smarttraveler.chat;

import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
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
    private final ChatMessageBuffer buffer;

    public RecommendedCourseService(JdbcTemplate jdbc, ObjectMapper mapper, StoredCourseCandidates candidates, ChatMessageBuffer buffer) {
        this.jdbc = jdbc; this.mapper = mapper; this.candidates = candidates; this.buffer = buffer;
    }

    /** 추천 코스를 메시지에 붙여 둔다. 저장은 화면의 [저장하기]를 눌렀을 때만 한다(피드백: 저장하기 Y/N). */
    public void record(long sessionId, long messageId, long sourceId) {
        String json = jdbc.queryForObject("SELECT conditions_json FROM chat_session_conditions WHERE session_id=?", String.class, sessionId);
        record(sessionId, messageId, sourceId, (ObjectNode) mapper.readTree(json));
    }

    /** used = 추천에 실제로 쓴 조건(말하지 않은 값은 기본값을 채운 사본). */
    public void record(long sessionId, long messageId, long sourceId, ObjectNode used) {
        record(sessionId, messageId, sourceId, used, List.of(sourceId));
    }

    /** candidateIds = 함께 보여준 추천 코스 전체(1위부터), 화면에서 저장할 코스를 고를 때 쓴다. */
    public void record(long sessionId, long messageId, long sourceId, ObjectNode used, List<Long> candidateIds) {
        buffer.setRecommendation(sessionId, messageId, mapper.writeValueAsString(buildSnapshot(sourceId, used, candidateIds)));
    }

    private ObjectNode reselect(ObjectNode snapshot, Long chosenCourseId) {
        if (snapshot.hasNonNull("savedCourseId")) throw alreadySavedError();
        if (chosenCourseId == null) return snapshot;
        List<Long> candidateIds = new java.util.ArrayList<>();
        snapshot.path("candidateCourseIds").forEach(id -> candidateIds.add(id.asLong()));
        if (!candidateIds.contains(chosenCourseId)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "선택한 코스가 추천 목록에 없습니다.");
        if (snapshot.path("sourceCourseId").asLong() == chosenCourseId) return snapshot;
        ObjectNode conditions = ((ObjectNode) snapshot.path("conditions")).deepCopy();
        return buildSnapshot(chosenCourseId, conditions, candidateIds);
    }

    private ObjectNode buildSnapshot(long sourceId, ObjectNode used, List<Long> candidateIds) {
        ObjectNode conditions = used;
        Integer requested = conditions.path("durationDays").isIntegralNumber() ? conditions.path("durationDays").asInt() : null;
        var rows = candidates.findCourse(sourceId, requested);
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.CONFLICT, "추천 코스를 다시 확인해 주세요.");
        var info = rows.getFirst();
        // 기간을 말하지 않고 기존 코스를 추천한 경우, 그 코스의 실제 일수를 저장용 조건에 채운다.
        int duration = requested != null ? requested : rows.stream().mapToInt(row -> ((Number) row.get("dayNo")).intValue()).max().orElse(0);
        conditions.put("durationDays", duration);
        ObjectNode snapshot = mapper.createObjectNode();
        snapshot.put("sourceCourseId", sourceId);
        snapshot.put("title", info.get("title").toString());
        snapshot.put("destination", info.get("destination").toString());
        snapshot.set("regionId", mapper.valueToTree(info.get("regionId")));
        snapshot.put("generationMethodId", jdbc.queryForObject("SELECT generation_method_id FROM course WHERE id=?", Integer.class, sourceId));
        snapshot.set("conditions", conditions);
        var candidateArray = snapshot.putArray("candidateCourseIds");
        candidateIds.forEach(candidateArray::add);
        var itinerary = snapshot.putArray("itinerary");
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
        return snapshot;
    }

    /** 기존 코스가 없을 때 숙소 주변 장소로 새로 짠 일정을 메시지에 붙인다(원본 코스 없음, 숙소 ID를 대신 기록). */
    public void recordLodgingItinerary(long sessionId, long messageId, ObjectNode used, List<Map<String, Object>> places) {
        int actualDays = places.stream().mapToInt(place -> (Integer) place.get("dayNo")).max().orElse(1);
        used.put("durationDays", actualDays);
        ObjectNode snapshot = mapper.createObjectNode();
        snapshot.putNull("sourceCourseId");
        snapshot.put("lodgingId", used.path("lodgingId").asLong());
        snapshot.put("title", used.path("lodgingName").asText("숙소") + " 주변 코스");
        snapshot.put("destination", used.path("regionName").asText(used.path("lodgingName").asText("")));
        snapshot.set("regionId", used.path("regionId"));
        snapshot.put("generationMethodId", 2);
        snapshot.set("conditions", used);
        var itinerary = snapshot.putArray("itinerary");
        Map<Integer, List<Map<String, Object>>> byDay = new java.util.TreeMap<>();
        for (var place : places) byDay.computeIfAbsent((Integer) place.get("dayNo"), key -> new java.util.ArrayList<>()).add(place);
        for (var entry : byDay.entrySet()) {
            ObjectNode day = itinerary.addObject();
            day.put("day_no", entry.getKey());
            day.put("theme", "숙소 주변 관광");
            var visits = day.putArray("visits");
            for (var place : entry.getValue()) {
                ObjectNode visit = visits.addObject();
                visit.put("seq", (Integer) place.get("seq"));
                visit.put("place_id", ((Number) place.get("placeId")).longValue());
                visit.put("place_name", (String) place.get("name"));
            }
        }
        buffer.setRecommendation(sessionId, messageId, mapper.writeValueAsString(snapshot));
    }

    public record SavedResult(long courseId, Long sidoId, Long sigunguId, String regionName) {}

    @Transactional
    public SavedResult save(long memberId, long sessionId, long messageId) {
        return save(memberId, sessionId, messageId, null);
    }

    @Transactional
    public SavedResult save(long memberId, long sessionId, long messageId, String startDateOverride) {
        return save(memberId, sessionId, messageId, startDateOverride, null);
    }

    @Transactional
    public SavedResult save(long memberId, long sessionId, long messageId, String startDateOverride, Long chosenCourseId) {
        // 아직 저장 전(보관함) 메시지: 대화 주인이 이 회원인지 확인하고 메모리의 추천 정보를 쓴다.
        if (buffer.isPending(sessionId, messageId)) {
            var owner = jdbc.query("SELECT member_id FROM chat_sessions WHERE session_id=? FOR UPDATE", (rs, row) -> rs.getObject(1, Long.class), sessionId);
            String pending = buffer.pendingRecommendation(sessionId, messageId).orElse(null);
            if (owner.isEmpty() || !Long.valueOf(memberId).equals(owner.getFirst()) || pending == null)
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "저장할 추천 코스가 없습니다.");
            ObjectNode snapshot = reselect((ObjectNode) mapper.readTree(pending), chosenCourseId);
            applyStartDateOverride(snapshot, startDateOverride);
            rejectDuplicate(memberId, snapshot);
            SavedResult saved = persist(memberId, snapshot, messageId);
            snapshot.put("savedCourseId", saved.courseId());
            buffer.setRecommendation(sessionId, messageId, mapper.writeValueAsString(snapshot));
            return saved;
        }
        messageId = buffer.resolve(sessionId, messageId);
        var rows = jdbc.query("""
            SELECT m.recommendation_json FROM chat_messages m JOIN chat_sessions s ON s.session_id=m.session_id
            WHERE m.message_id=? AND m.session_id=? AND s.member_id=? AND m.sender_role='ASSISTANT'
              AND m.recommendation_json IS NOT NULL FOR UPDATE
            """, (rs, row) -> rs.getString(1), messageId, sessionId, memberId);
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "저장할 추천 코스가 없습니다.");
        ObjectNode snapshot = reselect((ObjectNode) mapper.readTree(rows.getFirst()), chosenCourseId);
        applyStartDateOverride(snapshot, startDateOverride);
        rejectDuplicate(memberId, snapshot);
        SavedResult saved = persist(memberId, snapshot, messageId);
        snapshot.put("savedCourseId", saved.courseId());
        jdbc.update("UPDATE chat_messages SET recommendation_json=? WHERE message_id=?", mapper.writeValueAsString(snapshot), messageId);
        return saved;
    }

    /** 출발일 없이 추천한 일정을 저장할 때 날짜를 받아 채운다. */
    private void applyStartDateOverride(ObjectNode snapshot, String startDateOverride) {
        ObjectNode conditions = (ObjectNode) snapshot.path("conditions");
        if (!conditions.path("startDate").asText("").isBlank()) return;
        if (startDateOverride == null || startDateOverride.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "출발일을 입력해 주세요.");
        conditions.put("startDate", java.time.LocalDate.parse(startDateOverride).toString());
    }

    String guestSnapshot(String storedSnapshot, String startDate, Long courseId) {
        ObjectNode snapshot = reselect((ObjectNode) mapper.readTree(storedSnapshot), courseId);
        if (startDate != null) applyStartDateOverride(snapshot, startDate);
        return mapper.writeValueAsString(snapshot);
    }

    /** 추천 스냅샷을 회원의 course/course_day로 영속화하고 새 course id와 지역 정보를 반환한다. */
    private SavedResult persist(long memberId, ObjectNode snapshot, long messageId) {
        JsonNode conditions = snapshot.path("conditions");
        LocalDate start = LocalDate.parse(conditions.path("startDate").asText());
        LocalDate end = start.plusDays(conditions.path("durationDays").asInt() - 1L);
        ObjectNode metadata = mapper.createObjectNode();
        if (snapshot.hasNonNull("sourceCourseId")) metadata.put("source_course_id", snapshot.path("sourceCourseId").asLong());
        else metadata.putNull("source_course_id");
        if (snapshot.hasNonNull("lodgingId")) metadata.put("lodging_id", snapshot.path("lodgingId").asLong());
        metadata.put("chat_message_id", messageId); metadata.set("itinerary", snapshot.path("itinerary"));
        Long regionId = snapshot.path("regionId").isIntegralNumber() ? snapshot.path("regionId").asLong() : null;
        Object[] values = {memberId, snapshot.path("destination").asText(), start, end,
            conditions.path("adults").asInt(), conditions.path("children").asInt(),
            mapper.writeValueAsString(conditions.path("fixedEvents").isArray() ? conditions.path("fixedEvents") : List.of()),
            mapper.writeValueAsString(conditions), snapshot.path("title").asText(), snapshot.path("generationMethodId").asInt(),
            conditions.path("companionType").asText(), conditions.path("transportType").asText(), conditions.path("flowType").asText("COURSE_FIRST"),
            mapper.writeValueAsString(metadata), regionId};
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
            var dayKey = new GeneratedKeyHolder();
            jdbc.update(connection -> {
                var statement = connection.prepareStatement(
                    "INSERT INTO course_day(course_id,day_no,theme,visit_date) VALUES (?,?,?,?)", Statement.RETURN_GENERATED_KEYS);
                statement.setLong(1, courseId); statement.setInt(2, dayNo);
                statement.setString(3, day.path("theme").asText()); statement.setObject(4, start.plusDays(dayNo - 1L));
                return statement;
            }, dayKey);
            long courseDayId = Objects.requireNonNull(dayKey.getKey()).longValue();
            for (JsonNode visit : day.path("visits")) insertActivity(courseDayId, visit);
        }
        if (regionId == null) return new SavedResult(courseId, null, null, snapshot.path("destination").asText());
        var region = jdbc.queryForMap("SELECT region_id, parent_region_id, region_level, full_name FROM region WHERE region_id=?", regionId);
        boolean isSido = "SIDO".equals(region.get("region_level"));
        Long sidoId = isSido ? regionId : (Long) region.get("parent_region_id");
        Long sigunguId = isSido ? null : regionId;
        return new SavedResult(courseId, sidoId, sigunguId, (String) region.get("full_name"));
    }

    /** 추천 방문지를 activity 행으로도 저장해 마이페이지 상세·지도가 장소 좌표를 쓸 수 있게 한다. 시간은 10시부터 2시간 간격의 기본값이다. */
    private void insertActivity(long courseDayId, JsonNode visit) {
        int seq = visit.path("seq").asInt();
        long placeId = visit.path("place_id").asLong();
        String name = visit.path("place_name").asText();
        LocalTime startTime = LocalTime.of(Math.min(10 + (seq - 1) * 2, 22), 0);
        jdbc.update("""
            INSERT INTO activity(course_day_id,seq,start_time,title,category_id,place_name,place_id,
              address_snapshot,latitude_snapshot,longitude_snapshot)
            SELECT ?,?,?,?,CASE p.place_type WHEN 'CULTURE' THEN 2 WHEN 'LEISURE' THEN 3 WHEN 'SHOPPING' THEN 4
                     WHEN 'RESTAURANT' THEN 5 ELSE 1 END,?,p.place_id,p.address,p.latitude,p.longitude
            FROM (SELECT 1) one LEFT JOIN place p ON p.place_id=?
            """, courseDayId, seq, startTime, name, name, placeId);
    }

    /** 같은 원본 코스(또는 같은 숙소 기준 일정)를 같은 출발일로 이미 저장했으면 막는다 (피드백: 이미 추천된 코스 중복 적용 안 되게). */
    private void rejectDuplicate(long memberId, ObjectNode snapshot) {
        boolean lodgingBased = snapshot.hasNonNull("lodgingId");
        String column = lodgingBased ? "$.lodging_id" : "$.source_course_id";
        String value = lodgingBased ? String.valueOf(snapshot.path("lodgingId").asLong()) : String.valueOf(snapshot.path("sourceCourseId").asLong());
        Integer count = jdbc.queryForObject("""
            SELECT COUNT(*) FROM course
            WHERE member_no=? AND deleted_at IS NULL AND start_date=?
              AND JSON_VALUE(condition_meta,?)=?
            """, Integer.class, memberId, snapshot.path("conditions").path("startDate").asText(), column, value);
        if (count != null && count > 0) throw alreadySavedError();
    }

    private static ResponseStatusException alreadySavedError() {
        return new ResponseStatusException(HttpStatus.CONFLICT, "이미 저장한 코스예요. 저장된 여행에서 확인해 주세요.");
    }

    public JsonNode read(String json) { return json == null ? null : mapper.readTree(json); }
}
