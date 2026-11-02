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
@Table(name = "place", uniqueConstraints = {
        @UniqueConstraint(name = "UQ_place_source", columnNames = {"source_provider", "source_place_id"})
})
/** 관광 장소 */
public class Place {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "place_id", nullable = false)
    /** 장소ID */
    private Long placeId;

    @Column(name = "region_id", nullable = false)
    /** 지역ID */
    private Long regionId;

    @Column(name = "source_provider", nullable = false, length = 30)
    /** 데이터출처 */
    private String sourceProvider;

    @Column(name = "source_place_id", nullable = false, length = 100)
    /** 원본장소ID */
    private String sourcePlaceId;

    @Column(name = "place_type", nullable = false, length = 30)
    /** 장소유형 */
    private String placeType;

    @Column(name = "name", nullable = false, length = 200)
    /** 장소명 */
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    /** 장소소개 */
    private String description;

    @Column(name = "address", length = 300)
    /** 주소 */
    private String address;

    @Column(name = "address_detail", length = 200)
    /** 상세주소 */
    private String addressDetail;

    @Column(name = "latitude", precision = 10, scale = 7)
    /** 위도 */
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 10, scale = 7)
    /** 경도 */
    private BigDecimal longitude;

    @Column(name = "phone", length = 30)
    /** 전화번호 */
    private String phone;

    @Column(name = "homepage_url", length = 1000)
    /** 홈페이지 */
    private String homepageUrl;

    @Column(name = "opening_hours", columnDefinition = "TEXT")
    /** 이용시간 */
    private String openingHours;

    @Column(name = "closed_days", length = 500)
    /** 휴무일 */
    private String closedDays;

    @Column(name = "is_indoor")
    /** 실내여부 */
    private Boolean isIndoor;

    @Column(name = "recommended_minutes")
    /** 추천체류시간 */
    private Integer recommendedMinutes;

    @Column(name = "source_modified_at")
    /** 원본수정일시 */
    private LocalDateTime sourceModifiedAt;

    @Column(name = "last_synced_at")
    /** 최종동기화일시 */
    private LocalDateTime lastSyncedAt;

    @Column(name = "is_active", nullable = false)
    /** 사용여부 */
    private Boolean isActive;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at", nullable = false)
    /** 생성일시 */
    private LocalDateTime createdAt;

    @ColumnDefault("CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    @Column(name = "updated_at", nullable = false)
    /** 수정일시 */
    private LocalDateTime updatedAt;

    @Column(name = "verification_status", nullable = false, length = 20)
    /** 장소 검증 상태 */
    private String verificationStatus;

    @Column(name = "jibun_address", length = 300)
    /** 지번 주소 */
    private String jibunAddress;

    @Column(name = "kakao_place_url", length = 500)
    /** 카카오 장소 URL */
    private String kakaoPlaceUrl;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "region_id", referencedColumnName = "region_id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "fk_region_to_place"))
    private Region region;
}
