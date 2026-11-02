package com.smarttraveler.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@DynamicInsert
@DynamicUpdate
@Entity
@Table(name = "owner_review_history")
/** 업주 심사 이력 */
public class OwnerReviewHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id", nullable = false)
    /** 심사 이력 번호 */
    private Long reviewId;

    @Column(name = "owner_id", nullable = false)
    /** 업주 프로필 번호 */
    private Long ownerId;

    @Column(name = "reviewer_id")
    /** 심사자 회원번호 */
    private Long reviewerId;

    @Column(name = "review_status_code", nullable = false, length = 20)
    /** 심사 상태 */
    private String reviewStatusCode;

    @Column(name = "review_comment", length = 500)
    /** 심사 의견 */
    private String reviewComment;

    @Column(name = "reviewed_at")
    /** 심사일시 */
    private LocalDateTime reviewedAt;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at", nullable = false)
    /** 생성일시 */
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", referencedColumnName = "owner_id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "FK_owner_profiles_TO_owner_review_history"))
    private OwnerProfile owner;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "reviewer_id", referencedColumnName = "member_id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "FK_members_TO_owner_review_history"))
    private Member reviewer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "review_status_code", referencedColumnName = "review_status_code", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "FK_owner_review_status_TO_owner_review_history"))
    private OwnerReviewStatus reviewStatus;
}
