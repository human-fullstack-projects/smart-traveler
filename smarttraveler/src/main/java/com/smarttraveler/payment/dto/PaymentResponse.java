package com.smarttraveler.payment.dto;

public record PaymentResponse(
        Long paymentId,
        Long reservationId,
        String method,
        Integer amount,
        String status
) {
}
