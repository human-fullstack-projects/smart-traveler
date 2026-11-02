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
@Table(name = "related_attraction_rank", uniqueConstraints = {
        @UniqueConstraint(name = "UQ_related_attraction_rank_source_record_hash", columnNames = {"source_record_hash"})
})
/** 관광지 연관 장소 순위 */
public class RelatedAttractionRank {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "related_rank_id", nullable = false)
    /** 연관 순위 번호 */
    private Long relatedRankId;

    @Column(name = "base_ym", nullable = false, length = 6)
    /** 기준 연월(baseYm) */
    private String baseYm;

    @Column(name = "attraction_code", nullable = false, length = 64)
    /** 기준 관광지 코드(tAtsCd) */
    private String attractionCode;

    @Column(name = "attraction_name", length = 200)
    /** 기준 관광지명(tAtsNm) */
    private String attractionName;

    @Column(name = "area_code", length = 20)
    /** 기준 지역 코드(areaCd) */
    private String areaCode;

    @Column(name = "area_name", length = 100)
    /** 기준 지역명(areaNm) */
    private String areaName;

    @Column(name = "sigungu_code", length = 20)
    /** 기준 시군구 코드(signguCd) */
    private String sigunguCode;

    @Column(name = "sigungu_name", length = 100)
    /** 기준 시군구명(signguNm) */
    private String sigunguName;

    @Column(name = "related_code", nullable = false, length = 64)
    /** 연관 장소 코드(rlteTatsCd) */
    private String relatedCode;

    @Column(name = "related_name", length = 200)
    /** 연관 장소명(rlteTatsNm) */
    private String relatedName;

    @Column(name = "related_region_code", length = 20)
    /** 연관 지역 코드(rlteRegnCd) */
    private String relatedRegionCode;

    @Column(name = "related_region_name", length = 100)
    /** 연관 지역명(rlteRegnNm) */
    private String relatedRegionName;

    @Column(name = "related_sigungu_code", length = 20)
    /** 연관 시군구 코드(rlteSignguCd) */
    private String relatedSigunguCode;

    @Column(name = "related_sigungu_name", length = 100)
    /** 연관 시군구명(rlteSignguNm) */
    private String relatedSigunguName;

    @Column(name = "category_large", length = 100)
    /** 대분류명(rlteCtgryLclsNm) */
    private String categoryLarge;

    @Column(name = "category_medium", length = 100)
    /** 중분류명(rlteCtgryMclsNm) */
    private String categoryMedium;

    @Column(name = "category_small", length = 100)
    /** 소분류명(rlteCtgrySclsNm) */
    private String categorySmall;

    @Column(name = "related_rank")
    /** 연관 순위(rlteRank) */
    private Integer relatedRank;

    @Column(name = "fetched_at")
    /** 수집일시 */
    private LocalDateTime fetchedAt;

    @Column(name = "base_place_id")
    /** 기준 관광지 연결 */
    private Long basePlaceId;

    @Column(name = "related_place_id")
    /** 연관 장소 연결 */
    private Long relatedPlaceId;

    @Column(name = "source_record_hash", nullable = false, length = 64)
    /** 수집 원본 행 해시 */
    private String sourceRecordHash;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "base_place_id", referencedColumnName = "place_id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "FK_related_attraction_rank_base_place_id"))
    private Place basePlace;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "related_place_id", referencedColumnName = "place_id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "FK_related_attraction_rank_related_place_id"))
    private Place relatedPlace;
}
