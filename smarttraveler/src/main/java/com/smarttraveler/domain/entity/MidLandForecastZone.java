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
@Table(name = "mid_land_forecast_zone")
/** 중기 육상예보 구역 */
public class MidLandForecastZone {
    @Id
    @Column(name = "reg_id", nullable = false, length = 8)
    /** 예보구역코드 regId */
    private String regId;

    @Column(name = "zone_name", nullable = false, length = 100)
    /** 예보구역명 */
    private String zoneName;
}
