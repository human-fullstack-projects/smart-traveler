package com.smarttraveler.admin.data;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.*;
import java.math.BigDecimal;

@Entity @Table(name = "reservation") @Getter @Setter
public class Reservation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "reservation_id") private Long reservationId;
    @Column(name = "reservation_no", length = 64) private String reservationNo;
    @Column(name = "member_id") private Long memberId;
    @Column(name = "room_id") private Long roomId;
    @Column(name = "course_id") private Long courseId;
    @Column(name = "checkin_date") private LocalDate checkinDate;
    @Column(name = "checkout_date") private LocalDate checkoutDate;
    @Column(name = "guest_count") private Integer guestCount;
    @Column(name = "booker_name", length = 50) private String bookerName;
    @Column(name = "booker_phone", length = 30) private String bookerPhone;
    @Column(name = "request_memo", length = 500) private String requestMemo;
    @Column(name = "total_price") private Integer totalPrice;
    @Column(name = "status", length = 30) private String status;
    @Version @Column(name = "version") private Integer version;
    @Column(name = "cancel_reason", length = 500) private String cancelReason;
    @Column(name = "cancelled_at") private LocalDateTime cancelledAt;
    @Column(name = "created_at") private LocalDateTime createdAt;
    @Column(name = "updated_at") private LocalDateTime updatedAt;
}

