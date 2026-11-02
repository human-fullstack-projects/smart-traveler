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
@Table(name = "region", uniqueConstraints = {
        @UniqueConstraint(name = "UQ_region_h_code", columnNames = {"h_code"})
})
/** 지역 */
public class Region {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "region_id", nullable = false)
    /** 지역ID */
    private Long regionId;

    @Column(name = "parent_region_id")
    /** 상위지역ID */
    private Long parentRegionId;

    @Column(name = "region_code", nullable = false, length = 30)
    /** 지역코드 */
    private String regionCode;

    @Column(name = "region_name", nullable = false, length = 100)
    /** 지역명 */
    private String regionName;

    @Column(name = "full_name", nullable = false, length = 200)
    /** 전체지역명 */
    private String fullName;

    @Column(name = "region_level", nullable = false, length = 20)
    /** 지역단계 */
    private String regionLevel;

    @Column(name = "center_latitude", precision = 10, scale = 7)
    /** 중심위도 */
    private BigDecimal centerLatitude;

    @Column(name = "center_longitude", precision = 10, scale = 7)
    /** 중심경도 */
    private BigDecimal centerLongitude;

    @Column(name = "is_supported", nullable = false)
    /** 지원여부 */
    private Boolean isSupported;

    @Column(name = "weather_grid_id")
    /** 기상격자ID */
    private Long weatherGridId;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at", nullable = false)
    /** 생성일시 */
    private LocalDateTime createdAt;

    @ColumnDefault("CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    @Column(name = "updated_at", nullable = false)
    /** 수정일시 */
    private LocalDateTime updatedAt;

    @Column(name = "h_code", length = 10)
    /** 행정동 코드 */
    private String hCode;

    @Column(name = "sido_name", length = 20)
    /** 시도명 */
    private String sidoName;

    @Column(name = "sigungu_name", length = 30)
    /** 시군구명 */
    private String sigunguName;

    @Column(name = "emd_name", length = 30)
    /** 읍면동명 */
    private String emdName;

    @Column(name = "kma_nx")
    /** 기상 격자 X */
    private Short kmaNx;

    @Column(name = "kma_ny")
    /** 기상 격자 Y */
    private Short kmaNy;

    @Column(name = "abolished_at")
    /** 폐지일자 */
    private LocalDate abolishedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "weather_grid_id", referencedColumnName = "weather_grid_id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "fk_weather_grid_to_region"))
    private WeatherGrid weatherGrid;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "parent_region_id", referencedColumnName = "region_id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "FK_region_parent_region_id"))
    private Region parentRegion;
}
