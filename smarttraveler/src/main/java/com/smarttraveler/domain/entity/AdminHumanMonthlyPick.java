package com.smarttraveler.domain.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity @Table(name = "admin_human_monthly_pick", uniqueConstraints =
    @UniqueConstraint(name = "uq_admin_human_monthly_pick", columnNames = {"pick_month", "post_id"}))
@Getter @Setter
public class AdminHumanMonthlyPick {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "pick_month", nullable = false, length = 7) private String month;
    @Column(name = "post_id", nullable = false) private Long postId;
    @Column(name = "pick_rank", nullable = false) private Integer rank;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @Column(name = "created_by", nullable = false) private Long createdBy;
}
