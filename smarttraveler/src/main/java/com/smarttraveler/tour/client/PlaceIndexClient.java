package com.smarttraveler.tour.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/** 동기화한 장소를 ai-server(/internal/places/index)로 보내 임베딩시킨다. 실패해도 동기화 자체는 막지 않는다. */
@Component
public class PlaceIndexClient {
    private final ObjectMapper mapper;
    private final URI endpoint;
    /** HTTP/2 평문 업그레이드 시도가 uvicorn(h11)과 맞지 않아 POST 본문이 비어 전달되므로 HTTP/1.1로 고정한다. */
    private final HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).connectTimeout(Duration.ofSeconds(5)).build();

    public PlaceIndexClient(ObjectMapper mapper, @Value("${AI_SERVICE_URL:http://127.0.0.1:8001}") String baseUrl) {
        this.mapper = mapper;
        this.endpoint = URI.create(baseUrl.replaceAll("/+$", "") + "/internal/places/index");
    }

    /** rows는 100개 이하여야 한다(ai-server 쪽 상한). 실패하면 false만 반환한다. */
    public boolean index(List<Map<String, Object>> rows) {
        if (rows.isEmpty()) return true;
        String body = mapper.writeValueAsString(Map.of("rows", rows));
        HttpRequest request = HttpRequest.newBuilder(endpoint).timeout(Duration.ofSeconds(60))
                .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (IOException ex) {
            return false;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
