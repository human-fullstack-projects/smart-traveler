package com.smarttraveler.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.*;
import java.math.BigDecimal;

@Entity @Table(name = "admin_human_action_history") @Getter @Setter
public class HumanActionHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "id") private Long id;
    @Column(name = "course_id") private Long courseId;
    @Column(name = "report_id") private Long reportId;
    @Column(name = "admin_id") private Long adminId;
    @Column(name = "action") private String action;
    @Column(name = "note") private String note;
    @Column(name = "created_at") private LocalDateTime createdAt;
}

