package com.smarttraveler.admin.data;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.*;
import java.math.BigDecimal;

@Entity @Table(name = "owner_review_history") @Getter @Setter
public class OwnerReviewHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "review_id") private Long reviewId;
    @Column(name = "owner_id") private Long ownerId;
    @Column(name = "reviewer_id") private Long reviewerId;
    @Column(name = "review_status_code", length = 20) private String reviewStatusCode;
    @Column(name = "review_comment", length = 500) private String reviewComment;
    @Column(name = "reviewed_at") private LocalDateTime reviewedAt;
    @Column(name = "created_at") private LocalDateTime createdAt;
}

