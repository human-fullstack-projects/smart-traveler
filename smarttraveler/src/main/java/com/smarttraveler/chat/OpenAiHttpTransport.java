package com.smarttraveler.chat;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/** 서버 환경변수의 키로만 OpenAI에 요청한다. 키와 원문은 로그에 남기지 않는다. */
public class OpenAiHttpTransport implements OpenAiTransport {
    private final String apiKey;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    public OpenAiHttpTransport(@Value("${OPENAI_API_KEY:}") String apiKey) { this.apiKey = apiKey; }
    @Override
    public String request(String body) {
        if (apiKey == null || apiKey.isBlank()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "서버에 OpenAI API 키가 설정되지 않았습니다.");
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.openai.com/v1/responses"))
                .timeout(Duration.ofSeconds(30)).header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300)
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI 요청에 실패했습니다. 키, API 사용 권한 및 사용량을 확인해 주세요.");
            return response.body();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI 요청이 중단되었습니다.");
        } catch (java.io.IOException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI 서버에 연결할 수 없습니다.");
        }
    }
}
