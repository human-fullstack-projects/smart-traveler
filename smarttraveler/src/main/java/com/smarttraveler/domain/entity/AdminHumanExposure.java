package com.smarttraveler.domain.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity @Table(name = "admin_human_exposure", uniqueConstraints =
    @UniqueConstraint(name = "uq_admin_human_exposure_slot", columnNames = {"post_id", "scope", "region"}))
@Getter @Setter
public class AdminHumanExposure {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "post_id", nullable = false) private Long postId;
    @Column(nullable = false, length = 10) private String scope;
    @Column(nullable = false, length = 100) private String region;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @Column(name = "created_by") private Long createdBy;
}
