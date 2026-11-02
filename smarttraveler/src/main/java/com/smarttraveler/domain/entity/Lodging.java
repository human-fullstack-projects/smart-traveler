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
@Table(name = "lodging", uniqueConstraints = {
        @UniqueConstraint(name = "UQ_lodging_source_provider_source_content_id", columnNames = {"source_provider", "source_content_id"})
})
/** 숙소 */
public class Lodging {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "lodging_id", nullable = false)
    /** 숙소 ID */
    private Long lodgingId;

    @Column(name = "owner_id")
    /** 업주 ID */
    private Long ownerId;

    @Column(name = "region_id")
    /** 지역 ID */
    private Long regionId;

    @ColumnDefault("'OWNER'")
    @Column(name = "source_provider", nullable = false, length = 30)
    /** 데이터 출처 */
    private String sourceProvider;

    @Column(name = "source_content_id", length = 100)
    /** 원본 숙소 ID */
    private String sourceContentId;

    @Column(name = "lodging_type", nullable = false, length = 30)
    /** 숙소 유형 */
    private String lodgingType;

    @Column(name = "name", nullable = false, length = 200)
    /** 숙소명 */
    private String name;

    @Column(name = "address", nullable = false, length = 300)
    /** 주소 */
    private String address;

    @Column(name = "address_detail", length = 200)
    /** 상세 주소 */
    private String addressDetail;

    @Column(name = "zipcode", length = 10)
    /** 우편번호 */
    private String zipcode;

    @Column(name = "latitude", nullable = false, precision = 10, scale = 7)
    /** 위도 */
    private BigDecimal latitude;

    @Column(name = "longitude", nullable = false, precision = 10, scale = 7)
    /** 경도 */
    private BigDecimal longitude;

    @Column(name = "phone", length = 30)
    /** 전화번호 */
    private String phone;

    @Column(name = "homepage_url", length = 1000)
    /** 홈페이지 주소 */
    private String homepageUrl;

    @Column(name = "description", columnDefinition = "TEXT")
    /** 숙소 소개 */
    private String description;

    @Column(name = "checkin_time", nullable = false)
    /** 체크인 시간 */
    private LocalTime checkinTime;

    @Column(name = "checkout_time", nullable = false)
    /** 체크아웃 시간 */
    private LocalTime checkoutTime;

    @Column(name = "parking_info", length = 200)
    /** 주차 안내 */
    private String parkingInfo;

    @Column(name = "amenities", length = 500)
    /** 편의시설 */
    private String amenities;

    @Column(name = "refund_policy", columnDefinition = "TEXT")
    /** 취소·환불 규정 */
    private String refundPolicy;

    @ColumnDefault("'PENDING'")
    @Column(name = "status", nullable = false, length = 20)
    /** 숙소 상태 */
    private String status;

    @Column(name = "reject_reason", length = 500)
    /** 반려 사유 */
    private String rejectReason;

    @Column(name = "approved_at")
    /** 승인 시각 */
    private LocalDateTime approvedAt;

    @Column(name = "last_synced_at")
    /** 최종 수집일시 */
    private LocalDateTime lastSyncedAt;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at", nullable = false)
    /** 생성일시 */
    private LocalDateTime createdAt;

    @ColumnDefault("CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    @Column(name = "updated_at", nullable = false)
    /** 수정일시 */
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "owner_id", referencedColumnName = "owner_id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "FK_owner_TO_lodging_owner_id"))
    private OwnerProfile owner;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "region_id", referencedColumnName = "region_id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "FK_region_TO_lodging_region_id"))
    private Region region;
}
