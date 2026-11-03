package com.smarttraveler.admin.data;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.*;
import java.math.BigDecimal;

@Entity @Table(name = "room_payment") @Getter @Setter
public class Payment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "payment_id") private Long paymentId;
    @Column(name = "reservation_id") private Long reservationId;
    @Column(name = "idempotency_key", length = 100) private String idempotencyKey;
    @Column(name = "pg_provider", length = 30) private String pgProvider;
    @Column(name = "payment_key", length = 200) private String paymentKey;
    @Column(name = "method", length = 30) private String method;
    @Column(name = "amount") private Integer amount;
    @Column(name = "status", length = 30) private String status;
    @Column(name = "fail_reason", length = 500) private String failReason;
    @Column(name = "approved_at") private LocalDateTime approvedAt;
    @Column(name = "refund_amount") private Integer refundAmount;
    @Column(name = "refunded_at") private LocalDateTime refundedAt;
    @Column(name = "receipt_url", length = 1000) private String receiptUrl;
    @Column(name = "created_at") private LocalDateTime createdAt;
    @Column(name = "updated_at") private LocalDateTime updatedAt;
}

