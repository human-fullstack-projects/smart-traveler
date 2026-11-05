package com.smarttraveler.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity @Table(name = "admin_reservation_action_history") @Getter @Setter
public class ReservationActionHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "reservation_id") private Long reservationId;
    @Column(name = "admin_id") private Long adminId;
    @Column(name = "before_status") private String beforeStatus;
    @Column(name = "after_status") private String afterStatus;
    private String reason;
    @Column(name = "created_at") private LocalDateTime createdAt;
}
