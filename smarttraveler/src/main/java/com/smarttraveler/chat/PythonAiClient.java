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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;

/** 파이썬 AI 서버와 통신한다. OpenAI 키는 이 서버에서 읽지 않는다. */
@Component
public class PythonAiClient {
    private final ObjectMapper mapper;
    private final URI endpoint;
    /** HTTP/2 평문 업그레이드 시도가 uvicorn(h11)과 맞지 않아 POST 본문이 비어 전달되므로 HTTP/1.1로 고정한다. */
    private final HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).connectTimeout(Duration.ofSeconds(5)).build();
    public PythonAiClient(ObjectMapper mapper, @Value("${AI_SERVICE_URL:http://127.0.0.1:8001}") String baseUrl) {
        this.mapper = mapper;
        this.endpoint = URI.create(baseUrl.replaceAll("/+$", "") + "/internal/dialogue");
    }
    /** recommendConditions = 추천에 실제로 쓴 조건(말하지 않은 값은 기본값을 채운 사본). 추천이 아니면 null. */
    public record Result(String message, boolean confirmed, Long recommendedCourseId, ObjectNode recommendConditions) {
        public Result(String message, boolean confirmed) { this(message, confirmed, null, null); }
        public Result(String message, boolean confirmed, Long recommendedCourseId) { this(message, confirmed, recommendedCourseId, null); }
        public Result withMessage(String text, ObjectNode used) { return new Result(text, confirmed, recommendedCourseId, used); }
    }
    /** AI 서버 처리 방식: 조건만 읽기 / 주어진 후보로 바로 추천 / 예전 방식. */
    public enum Mode { EXTRACT_ONLY, FORCE_RECOMMEND, LEGACY }
    public Result process(String message, String previousQuestion, ObjectNode conditions, List<Map<String, Object>> candidates) {
        return process(message, previousQuestion, conditions, candidates, null);
    }
    public Result process(String message, String previousQuestion, ObjectNode conditions,
                          List<Map<String, Object>> candidates,
                          List<Map<String, Object>> courseCandidates) {
        return process(message, previousQuestion, conditions, candidates, courseCandidates, Map.of());
    }
    public Result process(String message, String previousQuestion, ObjectNode conditions,
                          List<Map<String, Object>> candidates, List<Map<String, Object>> courseCandidates,
                          Map<String, Object> savedPreferences) {
        return process(message, previousQuestion, conditions, candidates, courseCandidates, savedPreferences, Mode.LEGACY);
    }
    public Result process(String message, String previousQuestion, ObjectNode conditions,
                          List<Map<String, Object>> candidates, List<Map<String, Object>> courseCandidates,
                          Map<String, Object> savedPreferences, Mode mode) {
        Map<String, Object> payload = new LinkedHashMap<>();
        if (mode == Mode.EXTRACT_ONLY) payload.put("extractOnly", true);
        if (mode == Mode.FORCE_RECOMMEND) payload.put("forceRecommend", true);
        payload.put("message", message);
        payload.put("previousQuestion", previousQuestion);
        payload.put("conditions", conditions);
        payload.put("candidates", candidates);
        if (!savedPreferences.isEmpty()) payload.put("savedPreferences", savedPreferences);
        if (courseCandidates != null) {
            payload.put("courseCandidates", courseCandidates);
        }
        String body = mapper.writeValueAsString(payload);
        HttpRequest request = HttpRequest.newBuilder(endpoint).timeout(Duration.ofSeconds(75))
                .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI 서버가 요청을 처리하지 못했습니다.");
            JsonNode parsed = mapper.readTree(response.body());
            JsonNode changes = parsed.path("changes"), question = parsed.path("question");
            if (!changes.isObject() || !question.isTextual() || question.asText().isBlank() || question.asText().length() > 20000)
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI 서버 응답 형식이 올바르지 않습니다.");
            ConditionMerger.merge(changes, conditions);
            Long recommended = null;
            JsonNode courseId = parsed.path("recommendedCourseId");
            if (!courseId.isMissingNode() && !courseId.isNull()) {
                if (!courseId.isIntegralNumber() || !courseId.canConvertToLong() || courseId.asLong() <= 0 || courseCandidates == null
                    || courseCandidates.stream().noneMatch(row -> ((Number) row.get("courseId")).longValue() == courseId.asLong()))
                    throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "추천 코스가 후보와 일치하지 않습니다.");
                recommended = courseId.asLong();
            }
            return new Result(question.asText(), parsed.path("confirmed").asBoolean(false), recommended);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI 서버 요청이 중단되었습니다.");
        } catch (java.io.IOException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI 서버에 연결할 수 없습니다.");
        } catch (ResponseStatusException ex) { throw ex; }
        catch (RuntimeException ex) { throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI 서버 응답을 처리할 수 없습니다."); }
    }
}
