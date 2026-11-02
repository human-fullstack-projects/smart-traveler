package com.smarttraveler.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@DynamicInsert
@DynamicUpdate
@Entity
@Table(name = "room_payment_attempt", uniqueConstraints = {
        @UniqueConstraint(name = "UQ_room_payment_attempt_attempt_order_id", columnNames = {"attempt_order_id"}),
        @UniqueConstraint(name = "UQ_room_payment_attempt_idempotency_key", columnNames = {"idempotency_key"})
})
/** 객실 결제 시도 및 취소 이력 */
public class RoomPaymentAttempt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attempt_id", nullable = false)
    /** 결제 시도 번호 */
    private Long attemptId;

    @Column(name = "payment_id", nullable = false)
    /** 대표 결제 번호 */
    private Long paymentId;

    @Column(name = "attempt_type", nullable = false, length = 20)
    /** 시도 구분 */
    private String attemptType;

    @ColumnDefault("'READY'")
    @Column(name = "attempt_status", nullable = false, length = 20)
    /** 시도 상태 */
    private String attemptStatus;

    @Column(name = "attempt_order_id", nullable = false, length = 64)
    /** 시도별 주문번호 */
    private String attemptOrderId;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    /** 중복 요청 방지키 */
    private String idempotencyKey;

    @Column(name = "pg_payment_key", length = 200)
    /** 결제사 결제키 */
    private String pgPaymentKey;

    @Column(name = "amount", nullable = false)
    /** 시도 금액 */
    private Integer amount;

    @Column(name = "failure_reason", length = 500)
    /** 실패 사유 */
    private String failureReason;

    @Column(name = "processed_at")
    /** 결제사 처리일시 */
    private LocalDateTime processedAt;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at", nullable = false)
    /** 생성일시 */
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", referencedColumnName = "payment_id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "FK_room_payment_attempt_payment_id"))
    private RoomPayment payment;
}
