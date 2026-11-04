package com.smarttraveler.chat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class OpenAiConditionExtractorTest {
    private final ObjectMapper mapper = JsonMapper.builder().build();
    private String response(String json) {
        return mapper.writeValueAsString(Map.of("status", "completed", "output", java.util.List.of(Map.of("type", "message", "content", java.util.List.of(Map.of("type", "output_text", "text", json))))));
    }
    @Test void 명시된_조건을_병합하고_지역번호는_만들지_않는다() {
        String reply = """
                {"regionName":"부산","startDate":null,"durationDays":3,"adults":null,"children":null,"companionType":"FRIENDS","transportType":null,"preferences":["바다"]}
                """;
        String[] request = {null};
        var extractor = new OpenAiConditionExtractor(body -> { request[0] = body; return response(reply); }, mapper, "test-model");
        ObjectNode values = mapper.createObjectNode(); values.put("adults", 2); values.putNull("regionId");
        extractor.extract("친구와 부산 2박 3일, 바다가 좋아요", "", values);
        assertEquals("부산", values.path("regionName").asText());
        assertTrue(values.path("regionId").isNull());
        assertEquals(3, values.path("durationDays").asInt());
        assertEquals(2, values.path("adults").asInt());
        assertEquals("FRIENDS", values.path("companionType").asText());
        assertEquals("바다", values.path("preferences").get(0).asText());
        JsonNodeAssertions.check(mapper, request[0]);
    }
    @Test void 잘못된_값은_기존값을_덮어쓰지_않는다() {
        String reply = """
                {"regionName":null,"startDate":"2026-02-30","durationDays":99,"adults":-1,"children":-1,"companionType":"INVALID","transportType":"INVALID","preferences":null}
                """;
        var extractor = new OpenAiConditionExtractor(body -> response(reply), mapper, "test-model");
        ObjectNode values = mapper.createObjectNode(); values.put("startDate", "2026-12-10"); values.put("durationDays", 3); values.put("adults", 2); values.put("companionType", "FAMILY");
        extractor.extract("내용", "", values);
        assertEquals("2026-12-10", values.path("startDate").asText());
        assertEquals(3, values.path("durationDays").asInt());
        assertEquals(2, values.path("adults").asInt());
        assertEquals("FAMILY", values.path("companionType").asText());
    }
    @Test void 지역이_바뀌면_기존_지역번호를_제거한다() {
        String reply = """
                {"regionName":"제주","startDate":null,"durationDays":null,"adults":null,"children":null,"companionType":null,"transportType":null,"preferences":null}
                """;
        var extractor = new OpenAiConditionExtractor(body -> response(reply), mapper, "test-model");
        ObjectNode values = mapper.createObjectNode(); values.put("regionName", "부산"); values.put("regionId", 1);
        extractor.extract("제주로 바꿀게요", "", values);
        assertEquals("제주", values.path("regionName").asText()); assertTrue(values.path("regionId").isNull());
    }
    @Test void 거절이나_미완료_응답은_실패로_처리한다() {
        var extractor = new OpenAiConditionExtractor(body -> "{\"status\":\"incomplete\",\"output\":[]}", mapper, "test-model");
        var ex = assertThrows(ResponseStatusException.class, () -> extractor.extract("부산", "", mapper.createObjectNode()));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatusCode());
    }
    @Test void 키가_없으면_외부로_요청하지_않고_안내한다() {
        var transport = new OpenAiHttpTransport("");
        var ex = assertThrows(ResponseStatusException.class, () -> transport.request("{}"));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatusCode());
    }
    private static class JsonNodeAssertions {
        static void check(ObjectMapper mapper, String request) {
            var body = mapper.readTree(request);
            assertFalse(body.path("store").asBoolean());
            assertEquals("json_schema", body.path("text").path("format").path("type").asText());
            assertTrue(body.path("text").path("format").path("strict").asBoolean());
        }
    }
}
