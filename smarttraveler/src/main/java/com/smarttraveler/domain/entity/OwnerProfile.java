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
@Table(name = "owner_profiles", uniqueConstraints = {
        @UniqueConstraint(name = "UK_owner_profiles", columnNames = {"member_id"}),
        @UniqueConstraint(name = "UK_owner_profiles2", columnNames = {"business_number"})
})
/** 업주 프로필 */
public class OwnerProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "owner_id", nullable = false)
    /** 업주 프로필 번호 */
    private Long ownerId;

    @Column(name = "business_number", nullable = false, length = 20)
    /** 사업자 등록번호 */
    private String businessNumber;

    @Column(name = "business_name", nullable = false, length = 100)
    /** 상호명 */
    private String businessName;

    @Column(name = "representative_name", nullable = false, length = 50)
    /** 대표자명 */
    private String representativeName;

    @Column(name = "business_address", length = 255)
    /** 사업장 주소 */
    private String businessAddress;

    @Column(name = "business_phone", length = 20)
    /** 연락처 */
    private String businessPhone;

    @Column(name = "review_status_code", nullable = false, length = 20)
    /** 심사 상태 */
    private String reviewStatusCode;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at", nullable = false)
    /** 신청일시 */
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    /** 수정일시 */
    private LocalDateTime updatedAt;

    @Column(name = "member_id", nullable = false)
    /** 회원번호 */
    private Long memberId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", referencedColumnName = "member_id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "FK_members_TO_owner_profiles"))
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "review_status_code", referencedColumnName = "review_status_code", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "FK_owner_review_status_TO_owner_profiles"))
    private OwnerReviewStatus reviewStatus;
}
