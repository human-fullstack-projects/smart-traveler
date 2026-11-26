package com.smarttraveler.chat;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 파이썬 AI 서버와 통신한다. OpenAI 키는 이 서버에서 읽지 않는다.
 * ai-server가 메시지 한 건에서 지역·분위기·기간·동행·교통까지 전부 추출하고 벡터DB+RDB로 추천까지
 * 끝낸다(research/chat_test.py와 동일한 1회성 질의) — 스프링은 메시지와 이미 추천한 코스 id만
 * 전달하고, 조건을 단계별로 묻거나 합치는 일은 더 이상 하지 않는다. */
@Component
public class PythonAiClient {
    private final ObjectMapper mapper;
    private final URI dialogueEndpoint;
    private final URI lodgingEndpoint;
    /** HTTP/2 평문 업그레이드 시도가 uvicorn(h11)과 맞지 않아 POST 본문이 비어 전달되므로 HTTP/1.1로 고정한다. */
    private final HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).connectTimeout(Duration.ofSeconds(5)).build();

    public PythonAiClient(ObjectMapper mapper, @Value("${AI_SERVICE_URL:http://127.0.0.1:8001}") String baseUrl) {
        this.mapper = mapper;
        String base = baseUrl.replaceAll("/+$", "");
        this.dialogueEndpoint = URI.create(base + "/internal/dialogue");
        this.lodgingEndpoint = URI.create(base + "/internal/lodging-itinerary");
    }

    /** recommendedCourseIds = 함께 보여준 추천 코스 전체(1위부터 순서대로), 저장할 코스를 고를 때 쓴다.
     * recommendConditions = 저장 스냅샷에 들어갈 조건. ai-server가 더 이상 조건을 추출해 돌려주지
     * 않으므로, 코스 저장에 필요한 최소 기본값(ConditionDialogueService가 채움)만 담는다.
     * recommendedPlaces = 기존 코스 없이 새로 짠 일정(day·순서·장소·이름·주소, /internal/lodging-itinerary 전용). */
    public record Result(String message, Long recommendedCourseId, List<Long> recommendedCourseIds,
                          ObjectNode recommendConditions, List<Map<String, Object>> recommendedPlaces) {
        public Result(String message) { this(message, null, null, null, null); }
        public Result(String message, Long recommendedCourseId, List<Long> recommendedCourseIds) {
            this(message, recommendedCourseId, recommendedCourseIds, null, null);
        }
        public Result withMessage(String text, ObjectNode used) {
            return new Result(text, recommendedCourseId, recommendedCourseIds, used, recommendedPlaces);
        }
    }

    /** 메시지 한 건을 ai-server에 그대로 전달한다. excludedCourseIds는 이 대화에서 이미 추천한 코스라
     * 다시 추천하지 않도록 ai-server의 검색에서 빼는 용도다. */
    public Result process(String message, List<Long> excludedCourseIds) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("message", message);
        if (!excludedCourseIds.isEmpty()) payload.put("excludedCourseIds", excludedCourseIds);

        JsonNode parsed = post(dialogueEndpoint, payload);
        JsonNode text = parsed.path("message");
        if (!text.isTextual() || text.asText().isBlank() || text.asText().length() > 20000)
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI 서버 응답 형식이 올바르지 않습니다.");

        Long recommended = null;
        List<Long> recommendedIds = null;
        JsonNode courseId = parsed.path("recommendedCourseId");
        if (!courseId.isMissingNode() && !courseId.isNull()) {
            if (!courseId.isIntegralNumber() || !courseId.canConvertToLong() || courseId.asLong() <= 0)
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "추천 코스 번호가 올바르지 않습니다.");
            recommended = courseId.asLong();
            recommendedIds = new ArrayList<>();
            JsonNode idsNode = parsed.path("recommendedCourseIds");
            if (idsNode.isArray() && !idsNode.isEmpty()) {
                for (JsonNode item : idsNode) {
                    if (!item.isIntegralNumber() || !item.canConvertToLong() || item.asLong() <= 0)
                        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "추천 코스 번호가 올바르지 않습니다.");
                    recommendedIds.add(item.asLong());
                }
            } else {
                recommendedIds.add(recommended);
            }
        }
        return new Result(text.asText(), recommended, recommendedIds, null, null);
    }

    /** 숙소 기반 일정 추천(/internal/lodging-itinerary). 숙소 조회·주변 장소 조회는 ai-server가 직접 한다. */
    public Result recommendLodgingItinerary(long lodgingId, ObjectNode conditions) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("lodgingId", lodgingId);
        if (conditions.path("durationDays").isIntegralNumber()) payload.put("durationDays", conditions.path("durationDays").asInt());
        if (!conditions.path("startDate").asText("").isBlank()) payload.put("startDate", conditions.path("startDate").asText());
        List<String> preferences = new ArrayList<>();
        conditions.path("preferences").forEach(p -> preferences.add(p.asText("")));
        if (!preferences.isEmpty()) payload.put("preferences", preferences);

        JsonNode parsed = post(lodgingEndpoint, payload);
        JsonNode message = parsed.path("message");
        if (!message.isTextual() || message.asText().isBlank() || message.asText().length() > 20000)
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI 서버 응답 형식이 올바르지 않습니다.");

        List<Map<String, Object>> places = null;
        JsonNode placesNode = parsed.path("recommendedPlaces");
        if (placesNode.isArray() && !placesNode.isEmpty()) {
            places = new ArrayList<>();
            for (JsonNode item : placesNode) {
                JsonNode placeId = item.path("placeId");
                if (!placeId.isIntegralNumber() || !placeId.canConvertToLong() || placeId.asLong() <= 0)
                    throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "추천 장소 번호가 올바르지 않습니다.");
                Map<String, Object> place = new HashMap<>();
                place.put("dayNo", item.path("dayNo").asInt());
                place.put("seq", item.path("seq").asInt());
                place.put("placeId", placeId.asLong());
                place.put("name", item.path("name").asText());
                place.put("address", item.path("address").asText(""));
                places.add(place);
            }
        }
        return new Result(message.asText(), null, null, null, places);
    }

    private JsonNode post(URI endpoint, Map<String, Object> payload) {
        String body = mapper.writeValueAsString(payload);
        HttpRequest request = HttpRequest.newBuilder(endpoint).timeout(Duration.ofSeconds(75))
                .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI 서버가 요청을 처리하지 못했습니다.");
            return mapper.readTree(response.body());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI 서버 요청이 중단되었습니다.");
        } catch (java.io.IOException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI 서버에 연결할 수 없습니다.");
        } catch (ResponseStatusException ex) { throw ex; }
        catch (RuntimeException ex) { throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI 서버 응답을 처리할 수 없습니다."); }
    }
}
