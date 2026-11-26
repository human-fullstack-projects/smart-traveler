package com.smarttraveler.chat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Uses committed transactions so session cleanup is verified after an actual DB commit. */
@EnabledIfEnvironmentVariable(named = "RAG_DB_AUDIT", matches = "true")
@SpringBootTest(properties = {"COURSE_RAG_ENABLED=true", "spring.jpa.hibernate.ddl-auto=validate"})
class GuestRecommendedCourseDatabaseTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired ChatMessageBuffer buffer;
    @Autowired ObjectMapper mapper;
    @Autowired WebApplicationContext context;
    @Autowired com.smarttraveler.auth.repository.MemberRepository members;
    @Autowired com.smarttraveler.auth.service.SocialSessionLogin socialLogin;
    @MockitoBean PythonAiClient ai;
    MockMvc mvc;
    List<Long> chats = new ArrayList<>();
    List<String> accounts = new ArrayList<>();
    record Trip(MockHttpSession browser, long sessionId, long messageId) {}

    @BeforeEach void setup() { mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build(); }

    @AfterEach void cleanup() {
        SecurityContextHolder.clearContext();
        for (long chat : chats) {
            buffer.flush(chat); // 보관함에 남은 메시지도 DB에 써야 아래에서 함께 지운다
            var saved = jdbc.queryForList("SELECT id FROM course WHERE JSON_EXTRACT(condition_meta,'$.chat_message_id') IN (SELECT message_id FROM chat_messages WHERE session_id=?)", Long.class, chat);
            for (long course : saved) {
                jdbc.update("DELETE FROM activity WHERE course_day_id IN (SELECT id FROM course_day WHERE course_id=?)", course);
                jdbc.update("DELETE FROM course_day WHERE course_id=?", course);
                jdbc.update("DELETE FROM course WHERE id=?", course);
            }
            jdbc.update("DELETE FROM chat_session_conditions WHERE session_id=?", chat);
            jdbc.update("DELETE FROM chat_messages WHERE session_id=?", chat);
            jdbc.update("DELETE FROM chat_sessions WHERE session_id=?", chat);
        }
        for (String login : accounts) {
            jdbc.update("DELETE FROM member_consents WHERE member_id IN (SELECT member_id FROM members WHERE login_id=?)", login);
            jdbc.update("DELETE FROM members WHERE login_id=?", login);
        }
    }

    private long seedCourseId(String destinationPrefix, int duration) {
        return jdbc.queryForObject("""
            SELECT c.id FROM course c
            WHERE c.destination LIKE CONCAT(?, '%') AND c.status='done' AND c.is_active=1 AND c.deleted_at IS NULL
              AND DATEDIFF(c.end_date, c.start_date) + 1 = ?
              AND JSON_UNQUOTE(JSON_EXTRACT(c.condition_meta, '$.seed_batch')) IN ('smart-traveler-rag-1000-v1','smart-traveler-rag-2000-v1')
            ORDER BY c.id LIMIT 1
            """, Long.class, destinationPrefix, duration);
    }

    Trip recommend() throws Exception {
        var browser = new MockHttpSession();
        when(ai.process(anyString(), anyList())).thenReturn(new PythonAiClient.Result("조건을 알려주세요"));
        var created = mvc.perform(post("/api/chat/sessions").session(browser).with(csrf()).contentType("application/json")
            .content("{\"content\":\"부산 여행\"}")).andExpect(status().isOk()).andReturn();
        long id = mapper.readTree(created.getResponse().getContentAsString()).path("id").asLong();
        chats.add(id);
        ObjectNode conditions = mapper.createObjectNode();
        conditions.put("regionName", "부산"); conditions.put("durationDays", 2); conditions.put("startDate", "2026-12-10");
        conditions.put("adults", 2); conditions.put("children", 0); conditions.put("companionType", "FRIENDS"); conditions.put("transportType", "CAR");
        conditions.put("flowType", "COURSE_FIRST"); conditions.putArray("preferences"); conditions.putArray("fixedEvents");
        jdbc.update("UPDATE chat_session_conditions SET conditions_json=?,confirmed=TRUE WHERE session_id=?", mapper.writeValueAsString(conditions), id);
        long source = seedCourseId("부산", 2);
        when(ai.process(anyString(), anyList()))
            .thenReturn(new PythonAiClient.Result("추천 1 · 코스 #" + source, source, List.of(source)));
        var result = mvc.perform(post("/api/chat/sessions/" + id + "/messages").session(browser).with(csrf()).contentType("application/json")
            .content("{\"content\":\"추천해줘\"}")).andExpect(status().isOk()).andReturn();
        var messages = mapper.readTree(result.getResponse().getContentAsString()).path("messages");
        long message = messages.get(messages.size() - 1).path("id").asLong();
        assertTrue(messages.get(messages.size() - 1).path("recommendation").isObject());
        mvc.perform(post("/api/chat/sessions/" + id + "/recommendations/" + message + "/guest").session(browser).with(csrf()))
            .andExpect(status().isOk());
        return new Trip(browser, id, message);
    }

    String signup(MockHttpSession browser) throws Exception {
        String login = "rag_transfer_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        accounts.add(login);
        var response = mvc.perform(get("/api/auth/terms").session(browser)).andExpect(status().isOk()).andReturn();
        List<Map<String, Object>> consents = new ArrayList<>();
        for (var term : mapper.readTree(response.getResponse().getContentAsString())) consents.add(Map.of("termsId", term.path("termsId").asLong(), "agreed", true));
        String body = mapper.writeValueAsString(Map.of("role", "USER", "loginId", login, "password", "RagTest12345!",
            "nickname", "RAG 이전 테스트", "email", login + "@example.invalid", "consents", consents,
            "identityName", "RAG 테스트", "identityPhone", "010" + String.format("%08d", Math.floorMod(login.hashCode(), 100_000_000))));
        mvc.perform(post("/api/auth/signup").session(browser).with(csrf()).contentType("application/json").content(body)).andExpect(status().isCreated());
        return login;
    }

    @Test void signupLoginPreservesPendingAndClaimsExactlyOnce() throws Exception {
        Trip trip = recommend();
        mvc.perform(get("/api/chat/guest-course").session(trip.browser())).andExpect(status().isOk())
            .andExpect(jsonPath("$.course.messageId").value(trip.messageId()));
        mvc.perform(get("/api/chat/guest-course").session(new MockHttpSession())).andExpect(status().isOk()).andExpect(jsonPath("$.course").isEmpty());
        String login = signup(trip.browser());
        long member = members.findByLoginId(login).orElseThrow().getMemberId();
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM course WHERE member_no=?", Integer.class, member));
        String oldId = trip.browser().getId();
        mvc.perform(post("/api/auth/login").session(trip.browser()).with(csrf()).param("loginId", login).param("password", "RagTest12345!"))
            .andExpect(status().isOk());
        assertNotEquals(oldId, trip.browser().getId());
        var saved = mvc.perform(post("/api/chat/guest-course/claim").session(trip.browser()).with(csrf())).andExpect(status().isOk()).andReturn();
        long course = mapper.readTree(saved.getResponse().getContentAsString()).path("courseId").asLong();
        assertTrue(course > 0);
        assertEquals(member, jdbc.queryForObject("SELECT member_no FROM course WHERE id=?", Long.class, course));
        mvc.perform(post("/api/chat/guest-course/claim").session(trip.browser()).with(csrf())).andExpect(status().isOk()).andExpect(jsonPath("$.courseId").value(course));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM course WHERE member_no=?", Integer.class, member));
        assertNull(trip.browser().getAttribute(GuestRecommendedCourseService.PENDING));
        mvc.perform(get("/api/mypage/courses/course-" + course).session(trip.browser())).andExpect(status().isOk()).andExpect(jsonPath("$.days[0].places").isNotEmpty())
            .andExpect(jsonPath("$.days[0].mapPlaces").isNotEmpty());
        // 저장된 추천 코스는 방문지마다 activity 행(좌표 포함)을 가진다.
        assertEquals(0, jdbc.queryForObject("""
            SELECT COUNT(*) FROM course_day d LEFT JOIN activity a ON a.course_day_id=d.id
            WHERE d.course_id=? AND (a.id IS NULL OR a.latitude_snapshot IS NULL)""", Integer.class, course));
        // 저장 전(보관함) 메시지로 만든 코스는 대화를 DB에 쓸 때 메시지 번호가 실제 번호로 바뀐다.
        assertTrue(trip.messageId() < 0);
        buffer.flush(trip.sessionId());
        long realMessage = buffer.resolve(trip.sessionId(), trip.messageId());
        assertTrue(realMessage > 0);
        assertEquals(realMessage, jdbc.queryForObject("SELECT JSON_EXTRACT(condition_meta,'$.chat_message_id') FROM course WHERE id=?", Long.class, course));
        assertEquals(course, jdbc.queryForObject("SELECT JSON_EXTRACT(recommendation_json,'$.savedCourseId') FROM chat_messages WHERE message_id=?", Long.class, realMessage));
        mvc.perform(get("/api/chat/sessions/" + trip.sessionId()).session(trip.browser())).andExpect(status().isNotFound());
        mvc.perform(post("/api/auth/logout").session(trip.browser()).with(csrf())).andExpect(status().isNoContent());
        assertTrue(trip.browser().isInvalid());
    }

    @Test void failedInsertRollsBackAndCannotTransferToAnotherAccount() throws Exception {
        Trip trip = recommend();
        String first = signup(trip.browser());
        String second = signup(trip.browser());
        long firstId = members.findByLoginId(first).orElseThrow().getMemberId();
        // 이 시험은 DB에 저장된 메시지 경로: 먼저 대화를 저장하고, 화면이 들고 있던 임시 번호로 이전한다.
        buffer.flush(trip.sessionId());
        long realMessage = buffer.resolve(trip.sessionId(), trip.messageId());
        String json = jdbc.queryForObject("SELECT recommendation_json FROM chat_messages WHERE message_id=?", String.class, realMessage);
        ObjectNode broken = (ObjectNode) mapper.readTree(json);
        ((ObjectNode) broken.path("itinerary").get(1)).put("day_no", 1);
        jdbc.update("UPDATE chat_messages SET recommendation_json=? WHERE message_id=?", mapper.writeValueAsString(broken), realMessage);
        assertThrows(Exception.class, () -> mvc.perform(post("/api/chat/guest-course/claim").session(trip.browser()).with(user(first)).with(csrf())));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM course WHERE member_no=?", Integer.class, firstId));
        assertNotNull(trip.browser().getAttribute(GuestRecommendedCourseService.PENDING));
        assertNull(jdbc.queryForObject("SELECT member_id FROM chat_sessions WHERE session_id=?", Long.class, trip.sessionId()));
        mvc.perform(post("/api/chat/guest-course/claim").session(trip.browser()).with(user(second)).with(csrf())).andExpect(status().isConflict());
        jdbc.update("UPDATE chat_messages SET recommendation_json=? WHERE message_id=?", json, realMessage);
        mvc.perform(post("/api/chat/guest-course/claim").session(trip.browser()).with(user(first)).with(csrf())).andExpect(status().isOk());
        assertNull(trip.browser().getAttribute(GuestRecommendedCourseService.PENDING));
    }

    @Test void simultaneousClaimsAndSocialSessionRotationDoNotDuplicate() throws Exception {
        Trip trip = recommend();
        String login = signup(trip.browser());
        var request = new MockHttpServletRequest(); request.setSession(trip.browser());
        String old = trip.browser().getId();
        socialLogin.login(members.findByLoginId(login).orElseThrow(), request, new MockHttpServletResponse());
        SecurityContextHolder.clearContext();
        assertNotEquals(old, trip.browser().getId());
        var start = new java.util.concurrent.CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            java.util.concurrent.Callable<Long> claim = () -> {
                start.await();
                var result = mvc.perform(post("/api/chat/guest-course/claim").session(trip.browser()).with(csrf())).andExpect(status().isOk()).andReturn();
                return mapper.readTree(result.getResponse().getContentAsString()).path("courseId").asLong();
            };
            var first = pool.submit(claim); var second = pool.submit(claim); start.countDown();
            assertEquals(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS));
        }
        long member = members.findByLoginId(login).orElseThrow().getMemberId();
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM course WHERE member_no=?", Integer.class, member));
    }
}
