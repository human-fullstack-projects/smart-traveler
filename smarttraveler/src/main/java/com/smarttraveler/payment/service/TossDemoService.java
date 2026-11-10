package com.smarttraveler.payment.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;

/** Temporary, in-memory checkout for showing the Toss test flow without a reservation. */
@Service
public class TossDemoService {
    public record DemoOrder(String orderId, String orderName, int amount, String status,
                            String clientKey, String receiptUrl) {}

    private static final int MAX_AMOUNT = 10_000;
    private final Map<String, OrderState> orders = new ConcurrentHashMap<>();
    private final TossGateway toss;
    private final String clientKey;

    public TossDemoService(TossGateway toss, @Value("${app.toss.client-key:}") String clientKey) {
        this.toss = toss;
        this.clientKey = clientKey;
    }

    public DemoOrder create(String orderName, int amount) {
        if (!clientKey.startsWith("test_gck_")) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "토스 테스트 클라이언트 키가 설정되지 않았습니다.");
        }
        if (orderName == null || orderName.isBlank() || orderName.length() > 100
                || amount < 100 || amount > MAX_AMOUNT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "상품명 또는 테스트 금액을 확인해 주세요.");
        }
        orders.entrySet().removeIf(entry -> entry.getValue().expiresAt.isBefore(Instant.now()));
        String orderId = "ST-DEMO-" + UUID.randomUUID().toString().replace("-", "");
        OrderState state = new OrderState(orderId, orderName.trim(), amount);
        orders.put(orderId, state);
        return state.view(clientKey);
    }

    public DemoOrder confirm(String orderId, String paymentKey, int amount) {
        OrderState state = order(orderId);
        if (paymentKey == null || paymentKey.isBlank() || paymentKey.length() > 200 || amount != state.amount) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "결제 금액 또는 키가 주문과 다릅니다.");
        }
        synchronized (state) {
            if ("DONE".equals(state.status)) {
                if (!paymentKey.equals(state.paymentKey)) throw new ResponseStatusException(HttpStatus.CONFLICT, "다른 결제 키입니다.");
                return state.view(clientKey);
            }
            if (state.paymentKey != null && !paymentKey.equals(state.paymentKey)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "다른 결제 키로 이미 진행 중입니다.");
            }
            state.paymentKey = paymentKey;
            state.status = "IN_PROGRESS";
            JsonNode response;
            try {
                response = toss.confirm(paymentKey, orderId, state.amount, "demo-" + orderId);
            } catch (RestClientException failure) {
                // An approval can succeed even when its response is lost.
                try { response = toss.get(paymentKey); }
                catch (RestClientException lookupFailure) { return state.view(clientKey); }
            }
            apply(state, response);
            return state.view(clientKey);
        }
    }

    public DemoOrder get(String orderId) {
        OrderState state = order(orderId);
        synchronized (state) {
            if (state.paymentKey != null && !"DONE".equals(state.status)) {
                apply(state, toss.get(state.paymentKey));
            }
            return state.view(clientKey);
        }
    }

    private OrderState order(String orderId) {
        OrderState state = orders.get(orderId);
        if (state == null || state.expiresAt.isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "테스트 주문이 만료됐습니다. 다시 시작해 주세요.");
        }
        return state;
    }

    private void apply(OrderState state, JsonNode response) {
        if (response == null || !state.orderId.equals(response.path("orderId").asText())
                || !state.paymentKey.equals(response.path("paymentKey").asText())
                || state.amount != response.path("totalAmount").asInt(-1)) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "토스 결제 결과가 테스트 주문과 다릅니다.");
        }
        state.status = response.path("status").asText();
        state.receiptUrl = response.path("receipt").path("url").asText(null);
    }

    private static final class OrderState {
        final String orderId;
        final String orderName;
        final int amount;
        final Instant expiresAt = Instant.now().plus(30, ChronoUnit.MINUTES);
        String status = "READY";
        String paymentKey;
        String receiptUrl;

        OrderState(String orderId, String orderName, int amount) {
            this.orderId = orderId;
            this.orderName = orderName;
            this.amount = amount;
        }

        DemoOrder view(String clientKey) {
            return new DemoOrder(orderId, orderName, amount, status, clientKey, receiptUrl);
        }
    }
}
