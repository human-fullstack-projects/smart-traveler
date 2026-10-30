package com.smarttraveler.admin.data;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.*;
import java.math.BigDecimal;

@Entity @Table(name = "admin_human_report") @Getter @Setter
public class HumanReport {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "id") private Long id;
    @Column(name = "course_id") private Long courseId;
    @Column(name = "reporter_id") private Long reporterId;
    @Column(name = "reason") private String reason;
    @Column(name = "status") private String status;
    @Column(name = "created_at") private LocalDateTime createdAt;
    @Column(name = "resolved_at") private LocalDateTime resolvedAt;
}

