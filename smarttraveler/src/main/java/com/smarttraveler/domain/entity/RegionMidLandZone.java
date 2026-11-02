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
@Table(name = "region_mid_land_zone")
@IdClass(RegionMidLandZoneId.class)
/** 지역별 중기예보 구역 매핑 */
public class RegionMidLandZone {
    @Id
    @Column(name = "region_id", nullable = false)
    /** 지역 번호 */
    private Long regionId;

    @Id
    @Column(name = "reg_id", nullable = false, length = 8)
    /** 예보구역코드 */
    private String regId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "region_id", referencedColumnName = "region_id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "FK_region_mid_land_zone_region_id"))
    private Region region;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reg_id", referencedColumnName = "reg_id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "FK_region_mid_land_zone_reg_id"))
    private MidLandForecastZone reg;
}
