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
@Table(name = "room_reservation", uniqueConstraints = {
        @UniqueConstraint(name = "UQ_reservation_reservation_no", columnNames = {"reservation_no"}),
        @UniqueConstraint(name = "UQ_reservation_id_member", columnNames = {"member_id", "reservation_id"}),
        @UniqueConstraint(name = "UQ_reservation_id_course", columnNames = {"course_id", "reservation_id"}),
        @UniqueConstraint(name = "UQ_room_reservation_id_room", columnNames = {"reservation_id", "room_id"})
})
/** 객실예약 */
public class RoomReservation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reservation_id", nullable = false)
    /** 예약 ID */
    private Long reservationId;

    @Column(name = "reservation_no", nullable = false, length = 64)
    /** 예약번호 */
    private String reservationNo;

    @Column(name = "member_id", nullable = false)
    /** 여행자 회원 ID */
    private Long memberId;

    @Column(name = "room_id", nullable = false)
    /** 객실 ID */
    private Long roomId;

    @Column(name = "course_id")
    /** 연결 코스 ID */
    private Long courseId;

    @Column(name = "checkin_date", nullable = false)
    /** 체크인 날짜 */
    private LocalDate checkinDate;

    @Column(name = "checkout_date", nullable = false)
    /** 체크아웃 날짜 */
    private LocalDate checkoutDate;

    @Column(name = "guest_count", nullable = false)
    /** 인원 */
    private Integer guestCount;

    @Column(name = "booker_name", nullable = false, length = 50)
    /** 예약자 이름 */
    private String bookerName;

    @Column(name = "booker_phone", nullable = false, length = 30)
    /** 예약자 연락처 */
    private String bookerPhone;

    @Column(name = "request_memo", length = 500)
    /** 요청사항 */
    private String requestMemo;

    @Column(name = "total_price", nullable = false)
    /** 총 금액 */
    private Integer totalPrice;

    @ColumnDefault("'PENDING_PAYMENT'")
    @Column(name = "status", nullable = false, length = 30)
    /** 예약 상태 */
    private String status;

    @ColumnDefault("0")
    @Column(name = "version", nullable = false)
    /** 상태 버전 */
    private Integer version;

    @Column(name = "cancel_reason", length = 500)
    /** 취소 사유 */
    private String cancelReason;

    @Column(name = "cancelled_at")
    /** 취소 시각 */
    private LocalDateTime cancelledAt;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at", nullable = false)
    /** 생성일시 */
    private LocalDateTime createdAt;

    @ColumnDefault("CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    @Column(name = "updated_at", nullable = false)
    /** 수정일시 */
    private LocalDateTime updatedAt;

    @ColumnDefault("1")
    @Column(name = "room_quantity", nullable = false)
    /** 예약 객실 수 */
    private Integer roomQuantity;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", referencedColumnName = "member_id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "FK_member_TO_reservation_member_id"))
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", referencedColumnName = "room_id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "FK_room_TO_reservation_room_id"))
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "course_id", referencedColumnName = "id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "FK_course_TO_reservation_course_id"))
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumns(
            value = {
                    @JoinColumn(name = "course_id", referencedColumnName = "id", insertable = false, updatable = false),
                    @JoinColumn(name = "member_id", referencedColumnName = "member_no", insertable = false, updatable = false)
            },
            foreignKey = @ForeignKey(name = "FK_reservation_course_member")
    )
    private Course courseAndMember;
}
