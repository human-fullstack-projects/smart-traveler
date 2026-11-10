package com.smarttraveler.payment.controller;

import com.smarttraveler.payment.dto.PaymentResponse;
import com.smarttraveler.payment.dto.CheckoutResponse;
import com.smarttraveler.payment.service.PaymentService;
import com.smarttraveler.payment.service.TossPaymentService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;
    private final TossPaymentService tossPaymentService;

    public record ConfirmRequest(String orderId, String paymentKey, int amount) {}

    @PostMapping("/reservations/{reservationId}/prepare")
    public CheckoutResponse prepare(@PathVariable Long reservationId, Authentication auth) {
        return tossPaymentService.prepare(reservationId, auth);
    }

    @PostMapping("/confirm")
    public CheckoutResponse confirm(@RequestBody ConfirmRequest request, Authentication auth) {
        return tossPaymentService.confirm(request.orderId(), request.paymentKey(), request.amount(), auth);
    }

    @PostMapping("/reservations/{reservationId}/sync")
    public CheckoutResponse sync(@PathVariable Long reservationId, Authentication auth) {
        return tossPaymentService.sync(reservationId, auth);
    }

    @PostMapping("/webhook")
    public void webhook(@RequestBody String payload,
                        @RequestHeader("tosspayments-webhook-transmission-time") String transmissionTime,
                        @RequestHeader("tosspayments-webhook-signature") String signature) {
        tossPaymentService.webhook(payload, transmissionTime, signature);
    }

    @GetMapping("/{paymentId}")
    public PaymentResponse detail(@PathVariable Long paymentId, Authentication auth) {
        return paymentService.get(paymentId, auth);
    }

    @GetMapping("/reservation/{reservationId}")
    public List<PaymentResponse> listByReservation(@PathVariable Long reservationId, Authentication auth) {
        return paymentService.listByReservation(reservationId, auth);
    }
}
