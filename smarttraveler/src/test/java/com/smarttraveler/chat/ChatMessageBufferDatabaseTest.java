package com.smarttraveler.chat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 챗봇 메시지는 보관함에 모았다가 flush(대화 종료·페이지 이동·창 닫기) 때 DB에 한 번에 저장된다. */
@EnabledIfEnvironmentVariable(named = "RAG_DB_AUDIT", matches = "true")
@SpringBootTest(properties = {"COURSE_RAG_ENABLED=true", "spring.jpa.hibernate.ddl-auto=validate"})
class ChatMessageBufferDatabaseTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper mapper;
    @Autowired ChatMessageBuffer buffer;
    @Autowired WebApplicationContext context;
    @MockitoBean PythonAiClient ai;
    MockMvc mvc;
    final List<Long> chats = new ArrayList<>();

    @BeforeEach void setup() { mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build(); }

    @AfterEach void cleanup() {
        for (long chat : chats) {
            buffer.flush(chat);
            jdbc.update("DELETE FROM chat_session_conditions WHERE session_id=?", chat);
            jdbc.update("DELETE FROM chat_messages WHERE session_id=?", chat);
            jdbc.update("DELETE FROM chat_sessions WHERE session_id=?", chat);
        }
    }

    private void aiSays(String text) {
        when(ai.process(anyString(), anyList())).thenReturn(new PythonAiClient.Result(text));
    }
    private JsonNode post(MockHttpSession browser, String path, String content) throws Exception {
        var request = org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/chat" + path).session(browser).with(csrf());
        if (content != null) request.contentType("application/json").content(mapper.writeValueAsString(java.util.Map.of("content", content)));
        var result = mvc.perform(request).andExpect(status().isOk()).andReturn();
        return mapper.readTree(result.getResponse().getContentAsString());
    }
    private int saved(long chat) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM chat_messages WHERE session_id=?", Integer.class, chat);
    }
    private List<String> texts(JsonNode chat) {
        List<String> out = new ArrayList<>();
        for (JsonNode m : chat.path("messages")) out.add(m.path("role").asText() + ":" + m.path("content").asText());
        return out;
    }

    @Test void messagesStayInMemoryUntilFlushThenSaveInOrder() throws Exception {
        var browser = new MockHttpSession();
        aiSays("언제 출발하세요?");
        JsonNode created = post(browser, "/sessions", "부산 여행 가고 싶어요");
        long chat = created.path("id").asLong();
        chats.add(chat);
        assertEquals(3, created.path("messages").size());
        for (JsonNode m : created.path("messages")) assertTrue(m.path("id").asLong() < 0, "저장 전 메시지는 임시 번호");
        assertEquals(0, saved(chat), "대화 중에는 DB에 쓰지 않음");

        aiSays("몇 명이 가세요?");
        post(browser, "/sessions/" + chat + "/messages", "12월 10일에 가요");
        // ai-server는 메시지 한 건만 받는다(이전 질문 문맥을 따로 넘기지 않는다).
        verify(ai).process(eq("12월 10일에 가요"), eq(List.of()));
        assertEquals(0, saved(chat));

        var before = texts(mapper.readTree(mvc.perform(get("/api/chat/sessions/" + chat).session(browser))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()));
        assertEquals(5, before.size());

        JsonNode flushed = post(browser, "/sessions/" + chat + "/flush", null);
        assertEquals(5, saved(chat), "flush 때 한 번에 저장");
        assertEquals(before, texts(flushed), "순서·내용 그대로");
        for (JsonNode m : flushed.path("messages")) assertTrue(m.path("id").asLong() > 0, "저장 뒤에는 실제 번호");
        assertEquals(0, post(browser, "/sessions/" + chat + "/flush", null).path("messages").size() - 5, "다시 불러도 중복 저장 없음");
        assertEquals(5, saved(chat));

        aiSays("교통수단은요?");
        JsonNode next = post(browser, "/sessions/" + chat + "/messages", "2명이요");
        verify(ai).process(eq("2명이요"), eq(List.of()));
        assertEquals(7, next.path("messages").size());
        assertEquals(5, saved(chat));
    }

    @Test void failedReplyDoesNotKeepTheMessage() throws Exception {
        var browser = new MockHttpSession();
        aiSays("언제 출발하세요?");
        long chat = post(browser, "/sessions", "제주 여행").path("id").asLong();
        chats.add(chat);
        when(ai.process(anyString(), anyList())).thenThrow(new IllegalStateException("AI 서버 오류"));
        int code;
        try {
            code = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .post("/api/chat/sessions/" + chat + "/messages").session(browser).with(csrf())
                .contentType("application/json").content("{\"content\":\"내일이요\"}")).andReturn().getResponse().getStatus();
        } catch (Exception thrown) { code = 500; }
        assertTrue(code >= 400, "AI 오류는 실패 응답");
        var chatNow = mapper.readTree(mvc.perform(get("/api/chat/sessions/" + chat).session(browser)).andReturn().getResponse().getContentAsString());
        assertEquals(3, chatNow.path("messages").size(), "실패한 요청의 메시지는 남지 않음");
        assertEquals(3, buffer.flush(chat));
    }

    @Test void onlyTheOwnerCanSave() throws Exception {
        var browser = new MockHttpSession();
        aiSays("언제 출발하세요?");
        long chat = post(browser, "/sessions", "강릉 여행").path("id").asLong();
        chats.add(chat);
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/chat/sessions/" + chat + "/flush")
            .session(new MockHttpSession()).with(csrf())).andExpect(status().isNotFound());
        assertEquals(0, saved(chat));
    }
}
