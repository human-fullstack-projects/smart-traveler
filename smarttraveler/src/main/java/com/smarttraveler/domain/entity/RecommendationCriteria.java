package com.smarttraveler.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.*;
import java.math.BigDecimal;

@Entity @Table(name = "admin_recommendation_criteria") @Getter @Setter
public class RecommendationCriteria {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "id") private Long id;
    @Column(name = "name") private String name;
    @Column(name = "setting_value") private String value;
    @Column(name = "updated_at") private LocalDateTime updatedAt;
}

