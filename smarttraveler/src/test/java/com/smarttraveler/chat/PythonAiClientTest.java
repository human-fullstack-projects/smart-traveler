package com.smarttraveler.chat;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;

class PythonAiClientTest {
    @Test void optionalPreferenceContractAndLongCourseReplies() throws Exception {
        var mapper = JsonMapper.builder().build();
        var received = new java.util.ArrayList<tools.jackson.databind.JsonNode>();
        String answer = "저장 코스\n" + "DB 장소\n".repeat(1000);
        var selected = new java.util.concurrent.atomic.AtomicReference<Long>();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/internal/dialogue", exchange -> {
            received.add(mapper.readTree(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)));
            var payload = new java.util.LinkedHashMap<String, Object>(Map.of("changes", Map.of(), "question", answer, "confirmed", true));
            if (selected.get() != null) payload.put("recommendedCourseId", selected.get());
            byte[] body = mapper.writeValueAsString(payload).getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            var client = new PythonAiClient(mapper, "http://127.0.0.1:" + server.getAddress().getPort());
            var conditions = mapper.createObjectNode(); conditions.putArray("preferences").add("실내 위주");
            var result = client.process("네", "확인", conditions, List.of(), List.of(), Map.of("pace", "relaxed"));
            assertEquals(answer, result.message());
            assertEquals("relaxed", received.getFirst().path("savedPreferences").path("pace").asText());
            assertFalse(conditions.has("savedPreferences"));
            client.process("네", answer, conditions, List.of(), null);
            assertFalse(received.getLast().has("savedPreferences"));
            assertFalse(received.getLast().has("courseCandidates"));
            selected.set(1L);
            assertEquals(1L, client.process("추천해줘", "확인", conditions, List.of(), List.of(Map.of("courseId", 1L))).recommendedCourseId());
            selected.set(999L);
            assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> client.process("추천해줘", "확인", conditions, List.of(), List.of(Map.of("courseId", 1L))));
        } finally { server.stop(0); }
    }
}
