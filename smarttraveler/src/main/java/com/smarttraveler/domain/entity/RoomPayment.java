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
@Table(name = "room_payment", uniqueConstraints = {
        @UniqueConstraint(name = "UQ_payment_idempotency_key", columnNames = {"idempotency_key"}),
        @UniqueConstraint(name = "UQ_payment_payment_key", columnNames = {"payment_key"}),
        @UniqueConstraint(name = "UQ_room_payment_reservation_id", columnNames = {"reservation_id"}),
        @UniqueConstraint(name = "UQ_room_payment_order_id", columnNames = {"order_id"})
})
/** 객실결제 */
public class RoomPayment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id", nullable = false)
    /** 결제 ID */
    private Long paymentId;

    @Column(name = "reservation_id", nullable = false)
    /** 예약 ID */
    private Long reservationId;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    /** 중복 방지 키 */
    private String idempotencyKey;

    @ColumnDefault("'TOSS'")
    @Column(name = "pg_provider", nullable = false, length = 30)
    /** 결제사 */
    private String pgProvider;

    @Column(name = "payment_key", length = 200)
    /** 결제사 거래 키 */
    private String paymentKey;

    @Column(name = "method", length = 30)
    /** 결제 수단 */
    private String method;

    @Column(name = "amount", nullable = false)
    /** 결제 금액 */
    private Integer amount;

    @ColumnDefault("'READY'")
    @Column(name = "status", nullable = false, length = 30)
    /** 결제 상태 */
    private String status;

    @Column(name = "fail_reason", length = 500)
    /** 실패 사유 */
    private String failReason;

    @Column(name = "approved_at")
    /** 승인 시각 */
    private LocalDateTime approvedAt;

    @ColumnDefault("0")
    @Column(name = "refund_amount", nullable = false)
    /** 환불 금액 */
    private Integer refundAmount;

    @Column(name = "refunded_at")
    /** 환불 시각 */
    private LocalDateTime refundedAt;

    @Column(name = "receipt_url", length = 1000)
    /** 영수증 주소 */
    private String receiptUrl;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at", nullable = false)
    /** 생성일시 */
    private LocalDateTime createdAt;

    @ColumnDefault("CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    @Column(name = "updated_at", nullable = false)
    /** 수정일시 */
    private LocalDateTime updatedAt;

    @Column(name = "payer_member_id")
    /** 결제 회원 */
    private Long payerMemberId;

    @Column(name = "payer_name", nullable = false, length = 100)
    /** 결제자 이름 */
    private String payerName;

    @Column(name = "payer_phone", nullable = false, length = 30)
    /** 결제자 연락처 */
    private String payerPhone;

    @Column(name = "order_id", nullable = false, length = 64)
    /** 서버 주문번호 */
    private String orderId;

    @Column(name = "order_name", nullable = false, length = 100)
    /** 결제 상품명 */
    private String orderName;

    @Column(name = "payer_email", length = 255)
    /** 결제자 이메일 */
    private String payerEmail;

    @Column(name = "cancel_reason", length = 200)
    /** 결제 취소 사유 */
    private String cancelReason;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reservation_id", referencedColumnName = "reservation_id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "FK_reservation_TO_payment_reservation_id"))
    private RoomReservation reservation;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "payer_member_id", referencedColumnName = "member_id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "FK_room_payment_payer_member_id"))
    private Member payerMember;
}
