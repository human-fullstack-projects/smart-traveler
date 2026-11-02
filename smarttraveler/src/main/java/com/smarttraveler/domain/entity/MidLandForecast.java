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
@Table(name = "mid_land_forecast", uniqueConstraints = {
        @UniqueConstraint(name = "UQ_mid_land_forecast", columnNames = {"forecast_zone_code", "issued_at", "forecast_date", "forecast_period"})
})
/** 중기 육상 예보 */
public class MidLandForecast {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mid_forecast_id", nullable = false)
    /** 중기예보 번호 */
    private Long midForecastId;

    @Column(name = "forecast_zone_code", nullable = false, length = 8)
    /** 중기 예보구역코드 regId */
    private String forecastZoneCode;

    @Column(name = "issued_at", nullable = false)
    /** 발표 시각 tmFc (한국시간) */
    private LocalDateTime issuedAt;

    @Column(name = "forecast_date", nullable = false)
    /** 예보 대상 날짜 */
    private LocalDate forecastDate;

    @Column(name = "forecast_period", nullable = false, length = 3)
    /** 예보 구분 AM PM DAY */
    private String forecastPeriod;

    @Column(name = "precipitation_probability")
    /** 강수확률 퍼센트 rnSt */
    private Integer precipitationProbability;

    @Column(name = "weather_text", length = 100)
    /** 날씨예보 설명 wf */
    private String weatherText;

    @Column(name = "fetched_at", nullable = false)
    /** 수집 시각 */
    private LocalDateTime fetchedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "forecast_zone_code", referencedColumnName = "reg_id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "FK_mid_land_forecast_forecast_zone_code"))
    private MidLandForecastZone forecastZone;
}
