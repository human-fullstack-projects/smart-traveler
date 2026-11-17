package com.smarttraveler.chat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Opt in against the seeded local test DB. Reads only; never creates test accounts. */
@EnabledIfEnvironmentVariable(named = "RAG_DB_AUDIT", matches = "true")
@SpringBootTest(properties = {"COURSE_RAG_ENABLED=true", "spring.jpa.hibernate.ddl-auto=validate"})
class RagDatabaseAuditTest {
    @Autowired StoredCourseCandidates courses;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper mapper;
    @Autowired WebApplicationContext context;
    @Autowired com.smarttraveler.mypage.repository.MyPageRepository myPage;
    @MockitoBean PythonAiClient ai;

    @Test void busanOneToFourDaysUseOnlyValidSeedCourses() throws Exception {
        assertEquals(1000, jdbc.queryForObject("SELECT COUNT(*) FROM course WHERE JSON_UNQUOTE(JSON_EXTRACT(condition_meta, '$.seed_batch'))='smart-traveler-rag-1000-v1'", Integer.class));
        Map<String, List<Map<String, Object>>> snapshots = new LinkedHashMap<>();
        for (int duration = 1; duration <= 4; duration++) {
            var conditions = mapper.createObjectNode();
            conditions.put("regionName", "부산"); conditions.put("durationDays", duration);
            var rows = courses.find(conditions);
            assertFalse(rows.isEmpty(), "부산 " + duration + "일 후보");
            Map<Object, java.util.Set<Integer>> days = new LinkedHashMap<>();
            for (var row : rows) {
                assertNotNull(row.get("placeId")); assertNotNull(row.get("seq"));
                assertTrue(row.get("landscapes") instanceof List);
                days.computeIfAbsent(row.get("courseId"), key -> new java.util.HashSet<>()).add((Integer) row.get("dayNo"));
            }
            for (var actual : days.values()) assertEquals(duration, actual.size());
            snapshots.put(Integer.toString(duration), rows);
        }
        Path snapshot = Path.of("build", "rag-audit-candidates.json");
        Files.writeString(snapshot, mapper.writeValueAsString(snapshots));
    }

    @Test @Transactional
    void realSavedPreferencesAndAccountBoundariesThroughSecurityChain() throws Exception {
        var members = jdbc.queryForList("""
            SELECT member_id, login_id FROM members m
            WHERE login_id LIKE 'rag_seed_test_%' AND status='ACTIVE' AND deleted_at IS NULL
            AND NOT EXISTS (SELECT 1 FROM member_preference p WHERE p.member_no=m.member_id)
            AND NOT EXISTS (SELECT 1 FROM member_preference_tag t WHERE t.member_no=m.member_id)
            ORDER BY member_id LIMIT 2
            """);
        assertEquals(2, members.size(), "Two unconfigured seed accounts are needed; fixture writes roll back");
        long first = ((Number) members.getFirst().get("member_id")).longValue();
        long second = ((Number) members.getLast().get("member_id")).longValue();
        String firstLogin = members.getFirst().get("login_id").toString();
        String secondLogin = members.getLast().get("login_id").toString();
        saveFixture(first, "relaxed", "장소:야외", "선호지역:바다");
        saveFixture(second, "packed", "장소:실내", "선호지역:도시");
        when(ai.process(anyString(), anyString(), any(), anyList(), anyList(), anyMap()))
            .thenReturn(new PythonAiClient.Result("여행 조건을 입력해 주세요", false));
        var mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        var session = new MockHttpSession();
        String body = "{\"content\":\"부산 여행\"}";
        var guest = mvc.perform(post("/api/chat/sessions").session(session).with(csrf())
            .contentType("application/json").content(body)).andExpect(status().isOk()).andReturn();
        long guestId = mapper.readTree(guest.getResponse().getContentAsString()).path("id").asLong();
        mvc.perform(get("/api/chat/sessions/" + guestId).session(session).with(user(firstLogin)))
            .andExpect(status().isNotFound());
        var a = mvc.perform(post("/api/chat/sessions").session(session).with(user(firstLogin)).with(csrf())
            .contentType("application/json").content(body)).andExpect(status().isOk()).andReturn();
        long firstId = mapper.readTree(a.getResponse().getContentAsString()).path("id").asLong();
        mvc.perform(get("/api/chat/sessions/" + firstId).session(session).with(user(secondLogin)))
            .andExpect(status().isNotFound());
        var b = mvc.perform(post("/api/chat/sessions").session(session).with(user(secondLogin)).with(csrf())
            .contentType("application/json").content(body)).andExpect(status().isOk()).andReturn();
        long secondId = mapper.readTree(b.getResponse().getContentAsString()).path("id").asLong();
        mvc.perform(post("/api/chat/sessions/" + secondId + "/messages").session(session).with(user(secondLogin)).with(csrf())
            .contentType("application/json").content(body)).andExpect(status().isOk());
        mvc.perform(post("/api/chat/sessions/" + firstId + "/messages").session(session).with(user(secondLogin)).with(csrf())
            .contentType("application/json").content(body)).andExpect(status().isNotFound());
        verify(ai).process(anyString(), anyString(), any(), anyList(), anyList(), eq(Map.of()));
        verify(ai).process(anyString(), anyString(), any(), anyList(), anyList(),
            eq(Map.of("pace", "relaxed", "environment", "outdoor", "landscapes", List.of("sea"))));
        verify(ai, times(2)).process(anyString(), anyString(), any(), anyList(), anyList(),
            eq(Map.of("pace", "packed", "environment", "indoor", "landscapes", List.of("city"))));
    }

    private void saveFixture(long member, String pace, String environment, String landscape) {
        jdbc.update("INSERT INTO member_preference(member_no, pace, wake_up) VALUES (?, ?, 'normal')", member, pace);
        for (String name : List.of(environment, landscape)) {
            jdbc.update("INSERT INTO preference_tag(tag_type,name) VALUES ('style',?) ON DUPLICATE KEY UPDATE name=VALUES(name)", name);
            jdbc.update("INSERT INTO member_preference_tag(member_no,tag_id,polarity,weight) SELECT ?,id,'like',1 FROM preference_tag WHERE tag_type='style' AND name=?", member, name);
        }
    }

    @Test @Transactional
    void selectedRegionAndIntroAreAppliedWithoutGuessing() throws Exception {
        var mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        var session = new MockHttpSession();
        var response = mvc.perform(get("/api/chat/course-regions")).andExpect(status().isOk()).andReturn();
        var regions = mapper.readTree(response.getResponse().getContentAsString());
        assertFalse(regions.isEmpty());
        for (var region : regions) {
            assertTrue(jdbc.queryForObject("SELECT COUNT(*) FROM course WHERE destination=?", Integer.class, region.path("fullName").asText()) > 0);
        }
        var selected = regions.get(0);
        String body = mapper.writeValueAsString(Map.of("content", selected.path("fullName").asText() + " 지역으로 여행할게요.", "regionId", selected.path("regionId").asLong()));
        var created = mvc.perform(post("/api/chat/sessions").session(session).with(csrf()).contentType("application/json").content(body))
            .andExpect(status().isOk()).andReturn();
        var chat = mapper.readTree(created.getResponse().getContentAsString());
        assertEquals(ConditionQuestions.INTRO, chat.path("messages").get(0).path("content").asText());
        var conditions = mapper.readTree(chat.path("conditions").path("conditionsJson").asText());
        assertEquals(selected.path("regionId"), conditions.path("regionId"));
        assertEquals(selected.path("fullName"), conditions.path("regionName"));
        assertFalse(chat.path("conditions").path("confirmed").asBoolean());
        verifyNoInteractions(ai);
        mvc.perform(post("/api/chat/sessions").session(new MockHttpSession()).with(csrf()).contentType("application/json")
            .content("{\"content\":\"지역 선택\",\"regionId\":9223372036854775807}"))
            .andExpect(status().isBadRequest());
    }

    @Test @Transactional
    void memberCourseIsSavedImmediatelyWhenRecommendationIsDisplayed() throws Exception {
        var member = jdbc.queryForMap("SELECT member_id,login_id FROM members WHERE login_id LIKE 'rag_seed_test_%' AND status='ACTIVE' AND deleted_at IS NULL ORDER BY member_id LIMIT 1");
        long memberId = ((Number) member.get("member_id")).longValue();
        String login = member.get("login_id").toString();
        var mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        var session = new MockHttpSession();
        when(ai.process(anyString(), anyString(), any(), anyList(), anyList(), anyMap())).thenReturn(new PythonAiClient.Result("조건을 알려주세요", false));
        var created = mvc.perform(post("/api/chat/sessions").session(session).with(user(login)).with(csrf()).contentType("application/json")
            .content("{\"content\":\"부산 여행\"}")).andExpect(status().isOk()).andReturn();
        long sessionId = mapper.readTree(created.getResponse().getContentAsString()).path("id").asLong();
        var conditions = mapper.createObjectNode();
        conditions.put("regionName", "부산"); conditions.put("durationDays", 2); conditions.put("startDate", "2026-12-10");
        conditions.put("adults", 2); conditions.put("children", 0); conditions.put("companionType", "FRIENDS"); conditions.put("transportType", "CAR");
        conditions.put("flowType", "COURSE_FIRST"); conditions.putArray("preferences"); conditions.putArray("fixedEvents");
        jdbc.update("UPDATE chat_session_conditions SET conditions_json=?,confirmed=TRUE WHERE session_id=?", mapper.writeValueAsString(conditions), sessionId);
        long sourceId = ((Number) courses.find(conditions).getFirst().get("courseId")).longValue();
        long originalOwner = jdbc.queryForObject("SELECT member_no FROM course WHERE id=?", Long.class, sourceId);
        when(ai.process(anyString(), anyString(), any(), anyList(), anyList(), anyMap())).thenReturn(new PythonAiClient.Result("추천 1 · 코스 #" + sourceId, true, sourceId));
        int before = jdbc.queryForObject("SELECT COUNT(*) FROM course WHERE member_no=?", Integer.class, memberId);
        var recommended = mvc.perform(post("/api/chat/sessions/" + sessionId + "/messages").session(session).with(user(login)).with(csrf())
            .contentType("application/json").content("{\"content\":\"추천해줘\"}")).andExpect(status().isOk()).andReturn();
        var messages = mapper.readTree(recommended.getResponse().getContentAsString()).path("messages");
        var top = messages.get(messages.size() - 1);
        assertEquals(sourceId, top.path("recommendation").path("sourceCourseId").asLong());
        // 추천이 메시지로 출력된 시점에 이미 회원의 course로 저장되어 있어야 한다 (버튼 클릭 불필요).
        long savedId = top.path("recommendation").path("savedCourseId").asLong();
        assertTrue(savedId > 0);
        assertEquals(before + 1, jdbc.queryForObject("SELECT COUNT(*) FROM course WHERE member_no=?", Integer.class, memberId));
        assertEquals(memberId, jdbc.queryForObject("SELECT member_no FROM course WHERE id=?", Long.class, savedId));
        // 명시적 저장 엔드포인트를 다시 호출해도(예: 재시도) 같은 courseId를 반환하고 중복 저장되지 않는다.
        String saveUrl = "/api/chat/sessions/" + sessionId + "/recommendations/" + top.path("id").asLong() + "/save";
        var repeated = mvc.perform(post(saveUrl).session(session).with(user(login)).with(csrf())).andExpect(status().isOk()).andReturn();
        assertEquals(savedId, mapper.readTree(repeated.getResponse().getContentAsString()).path("courseId").asLong());
        assertEquals(before + 1, jdbc.queryForObject("SELECT COUNT(*) FROM course WHERE member_no=?", Integer.class, memberId));
        assertEquals(originalOwner, jdbc.queryForObject("SELECT member_no FROM course WHERE id=?", Long.class, sourceId));
        var detail = myPage.course(memberId, "course-" + savedId);
        assertEquals(2, detail.days().size());
        assertTrue(detail.days().stream().allMatch(day -> !day.places().isEmpty()));
        assertEquals(java.time.LocalDate.of(2026, 12, 11), detail.days().getLast().date());
        mvc.perform(post(saveUrl).session(new MockHttpSession()).with(csrf())).andExpect(status().isUnauthorized());
        mvc.perform(post(saveUrl).session(new MockHttpSession()).with(user("rag_seed_test_002")).with(csrf())).andExpect(status().isNotFound());
    }
}
