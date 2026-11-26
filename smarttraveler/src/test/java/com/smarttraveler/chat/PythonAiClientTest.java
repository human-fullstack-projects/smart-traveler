package com.smarttraveler.chat;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;
import static org.junit.jupiter.api.Assertions.*;

/** ai-server가 메시지 한 건에서 지역·분위기·기간·동행·교통까지 전부 추출하고 벡터DB+RDB로
 * 추천까지 끝내는 1회성 질의(research/chat_test.py와 동일)로 바뀐 뒤의 요청/응답 계약을 검증한다. */
class PythonAiClientTest {
    private final JsonMapper mapper = JsonMapper.builder().build();
    private HttpServer server;
    private volatile JsonNode lastRequest;
    private volatile String nextResponseBody;
    private volatile int nextStatus = 200;

    @BeforeEach
    void startFakeAiServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/internal/dialogue", this::handle);
        server.createContext("/internal/lodging-itinerary", this::handle);
        server.start();
    }

    @AfterEach
    void stopFakeAiServer() { server.stop(0); }

    private void handle(com.sun.net.httpserver.HttpExchange exchange) throws java.io.IOException {
        lastRequest = mapper.readTree(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        byte[] body = nextResponseBody.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(nextStatus, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }

    private PythonAiClient client() {
        return new PythonAiClient(mapper, "http://127.0.0.1:" + server.getAddress().getPort());
    }

    @Test
    void 요청에_메시지와_제외코스만_담는다() {
        nextResponseBody = """
            {"message": "추천 1 · 코스 #1"}
            """;
        client().process("추천해줘", List.of(10L, 20L));

        assertEquals("추천해줘", lastRequest.path("message").asText());
        assertEquals(List.of(10L, 20L), toLongList(lastRequest.path("excludedCourseIds")));
        assertFalse(lastRequest.has("conditions"));
        assertFalse(lastRequest.has("previousQuestion"));
    }

    @Test
    void 제외코스가_없으면_필드를_아예_보내지_않는다() {
        nextResponseBody = """
            {"message": "질문"}
            """;
        client().process("메시지", List.of());

        assertFalse(lastRequest.has("excludedCourseIds"));
    }

    @Test
    void 추천코스와_목록을_결과에_담는다() {
        nextResponseBody = """
            {"message": "코스 #1을 추천해요", "recommendedCourseId": 1, "recommendedCourseIds": [1, 2]}
            """;
        var result = client().process("추천해줘", List.of());

        assertEquals(1L, result.recommendedCourseId());
        assertEquals(List.of(1L, 2L), result.recommendedCourseIds());
        assertEquals("코스 #1을 추천해요", result.message());
    }

    @Test
    void 추천코스가_없으면_null이다() {
        nextResponseBody = """
            {"message": "조건에 맞는 저장된 코스를 찾지 못했어요."}
            """;
        var result = client().process("메시지", List.of());

        assertNull(result.recommendedCourseId());
        assertNull(result.recommendedCourseIds());
    }

    @Test
    void 숙소_일정_요청은_lodgingId와_필요한_조건만_전달한다() {
        nextResponseBody = """
            {"message": "일정이에요", "recommendedPlaces": [
                {"dayNo": 1, "seq": 1, "placeId": 11, "name": "해운대", "address": "부산"}
            ]}
            """;
        ObjectNode conditions = mapper.createObjectNode();
        conditions.put("durationDays", 2);
        conditions.put("startDate", "2026-12-10");
        conditions.putArray("preferences").add("바다");

        var result = client().recommendLodgingItinerary(7L, conditions);

        assertEquals(7L, lastRequest.path("lodgingId").asLong());
        assertEquals(2, lastRequest.path("durationDays").asInt());
        assertEquals("2026-12-10", lastRequest.path("startDate").asText());
        assertEquals("바다", lastRequest.path("preferences").get(0).asText());

        assertEquals("일정이에요", result.message());
        assertEquals(1, result.recommendedPlaces().size());
        var place = result.recommendedPlaces().get(0);
        assertEquals("해운대", place.get("name"));
        assertEquals(11L, place.get("placeId"));
    }

    @Test
    void 숙소_일정_응답에_장소번호가_없으면_서버오류로_처리한다() {
        nextResponseBody = """
            {"message": "일정이에요", "recommendedPlaces": [{"dayNo": 1, "seq": 1, "name": "해운대"}]}
            """;
        assertThrows(org.springframework.web.server.ResponseStatusException.class,
            () -> client().recommendLodgingItinerary(7L, mapper.createObjectNode()));
    }

    @Test
    void ai서버가_200이_아니면_503으로_감싼다() {
        nextStatus = 503;
        nextResponseBody = "{}";
        assertThrows(org.springframework.web.server.ResponseStatusException.class,
            () -> client().process("추천해줘", List.of()));
    }

    @Test
    void 메시지가_비어있으면_서버오류로_처리한다() {
        nextResponseBody = """
            {"message": ""}
            """;
        assertThrows(org.springframework.web.server.ResponseStatusException.class,
            () -> client().process("추천해줘", List.of()));
    }

    private static List<Long> toLongList(JsonNode array) {
        List<Long> values = new java.util.ArrayList<>();
        array.forEach(item -> values.add(item.asLong()));
        return values;
    }
}
