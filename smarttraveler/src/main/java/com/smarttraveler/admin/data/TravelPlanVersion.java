package com.smarttraveler.admin.data;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.*;
import java.math.BigDecimal;

@Entity @Table(name = "travel_plan_version") @Getter @Setter
public class TravelPlanVersion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "plan_version_id") private Long planVersionId;
    @Column(name = "plan_id") private Long planId;
    @Column(name = "title") private String title;
    @Column(name = "summary") private String summary;
    @Column(name = "generation_status") private String generationStatus;
    @Column(name = "error_message") private String errorMessage;
    @Column(name = "model") private String model;
    @Column(name = "created_at") private LocalDateTime createdAt;
}

