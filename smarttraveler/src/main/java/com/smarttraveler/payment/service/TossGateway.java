package com.smarttraveler.payment.service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;

@Component
public class TossGateway {
    private final RestClient client = RestClient.create("https://api.tosspayments.com");
    private final String secretKey;

    public TossGateway(@Value("${app.toss.secret-key:}") String secretKey) {
        this.secretKey = secretKey;
    }

    public JsonNode confirm(String paymentKey, String orderId, int amount, String idempotencyKey) {
        return client.post().uri("/v1/payments/confirm")
                .headers(headers -> {
                    headers.setBasicAuth(encodedKey());
                    headers.set("Idempotency-Key", idempotencyKey);
                })
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("paymentKey", paymentKey, "orderId", orderId, "amount", amount))
                .retrieve().body(JsonNode.class);
    }

    public JsonNode get(String paymentKey) {
        return client.get().uri("/v1/payments/{paymentKey}", paymentKey)
                .headers(headers -> headers.setBasicAuth(encodedKey()))
                .retrieve().body(JsonNode.class);
    }

    private String encodedKey() {
        if (!secretKey.startsWith("test_gsk_")) {
            throw new IllegalStateException("토스 테스트 시크릿 키를 서버 환경변수에 설정해 주세요.");
        }
        return Base64.getEncoder().encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));
    }
}
