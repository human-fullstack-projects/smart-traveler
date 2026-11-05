package com.smarttraveler.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity @Table(name = "admin_recommendation_criteria_history") @Getter @Setter
public class RecommendationCriteriaHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "criteria_id") private Long criteriaId;
    @Column(name = "admin_id") private Long adminId;
    @Column(name = "old_value") private String oldValue;
    @Column(name = "new_value") private String newValue;
    @Column(name = "created_at") private LocalDateTime createdAt;
}
