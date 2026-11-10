package com.smarttraveler.payment.dto;

public record CheckoutResponse(Long reservationId, String orderId, String orderName, int amount,
                               String status, String reservationStatus, String clientKey, String receiptUrl) {}
