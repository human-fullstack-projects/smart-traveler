package com.smarttraveler.payment.controller;

import com.smarttraveler.payment.dto.PaymentResponse;
import com.smarttraveler.payment.service.PaymentService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;

    @GetMapping("/{paymentId}")
    public PaymentResponse detail(@PathVariable Long paymentId) {
        return paymentService.get(paymentId);
    }

    @GetMapping("/reservation/{reservationId}")
    public List<PaymentResponse> listByReservation(@PathVariable Long reservationId) {
        return paymentService.listByReservation(reservationId);
    }
}
