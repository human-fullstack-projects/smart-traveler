package com.smarttraveler.admin.data;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.*;
import java.math.BigDecimal;

@Entity @Table(name = "human_course_report") @Getter @Setter
public class HumanReport {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "report_id") private Long id;
    @Column(name = "post_id") private Long courseId;
    @Column(name = "reporter_id") private Long reporterId;
    @Column(name = "reason", nullable = false, length = 500) private String reason;
    @Column(name = "status", nullable = false, length = 20) private String status;
    @Column(name = "created_at") private LocalDateTime createdAt;
    @Column(name = "processed_at") private LocalDateTime resolvedAt;
    @Column(name = "processed_by") private Long processedBy;
    @Column(name = "resolution", length = 500) private String resolution;
}

