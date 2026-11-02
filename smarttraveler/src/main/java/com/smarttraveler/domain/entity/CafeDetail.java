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
@Table(name = "cafe_detail")
/** 카페 상세 */
public class CafeDetail {
    @Id
    @Column(name = "place_id", nullable = false)
    /** 장소 번호 */
    private Long placeId;

    @Column(name = "cafe_type", length = 50)
    /** 카페 유형 */
    private String cafeType;

    @Column(name = "franchise_name", length = 100)
    /** 브랜드명 */
    private String franchiseName;

    @Column(name = "signature_menu", length = 300)
    /** 대표 메뉴 */
    private String signatureMenu;

    @Column(name = "price_range", length = 20)
    /** 가격대 */
    private String priceRange;

    @Column(name = "seat_size", length = 10)
    /** 좌석 규모 */
    private String seatSize;

    @Column(name = "study_ok")
    /** 공부 가능 */
    private Boolean studyOk;

    @Column(name = "outlet_yn")
    /** 콘센트 여부 */
    private Boolean outletYn;

    @Column(name = "wifi_yn")
    /** 와이파이 여부 */
    private Boolean wifiYn;

    @Column(name = "pet_ok")
    /** 반려동물 가능 */
    private Boolean petOk;

    @Column(name = "terrace_yn")
    /** 테라스 여부 */
    private Boolean terraceYn;

    @Column(name = "parking")
    /** 주차 가능 */
    private Boolean parking;

    @Column(name = "no_kids_zone")
    /** 노키즈존 여부 */
    private Boolean noKidsZone;

    @Column(name = "view_type", length = 30)
    /** 전망 유형 */
    private String viewType;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at", nullable = false)
    /** 생성일시 */
    private LocalDateTime createdAt;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "updated_at", nullable = false)
    /** 수정일시 */
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "place_id", referencedColumnName = "place_id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "FK_cafe_detail_place_id"))
    private Place place;
}
