package com.smarttraveler.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.smarttraveler.payment.service.TossDemoService;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class TossDemoServiceTest {
    private final TossDemoService service = new TossDemoService(null, "test_gck_docs_example");

    @Test
    void createsServerOwnedTestAmountAndRejectsTampering() {
        var order = service.create("스마트 트래블 테스트", 1000);
        assertEquals(1000, order.amount());
        assertEquals("READY", order.status());
        assertThrows(ResponseStatusException.class, () -> service.confirm(order.orderId(), "payment-key", 2000));
        assertEquals("READY", service.get(order.orderId()).status());
    }

    @Test
    void limitsDemoAmount() {
        assertThrows(ResponseStatusException.class, () -> service.create("테스트", 10001));
    }
}
