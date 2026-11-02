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
@Table(name = "weather_forecast", uniqueConstraints = {
        @UniqueConstraint(name = "UQ_weather_grid_issue_forecast", columnNames = {"weather_grid_id", "issued_at", "forecast_at"})
})
/** 날씨 예보 */
public class WeatherForecast {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "weather_forecast_id", nullable = false)
    /** 예보ID */
    private Long weatherForecastId;

    @Column(name = "weather_grid_id", nullable = false)
    /** 기상격자ID */
    private Long weatherGridId;

    @Column(name = "issued_at", nullable = false)
    /** 예보발표시각 */
    private LocalDateTime issuedAt;

    @Column(name = "forecast_at", nullable = false)
    /** 예보대상시각 */
    private LocalDateTime forecastAt;

    @Column(name = "temperature_c", precision = 5, scale = 1)
    /** 기온 */
    private BigDecimal temperatureC;

    @Column(name = "precipitation_probability")
    /** 강수확률 */
    private Integer precipitationProbability;

    @Column(name = "precipitation_mm", precision = 7, scale = 2)
    /** 강수량 */
    private BigDecimal precipitationMm;

    @Column(name = "precipitation_type", length = 30)
    /** 강수형태 */
    private String precipitationType;

    @Column(name = "sky_condition", length = 30)
    /** 하늘상태 */
    private String skyCondition;

    @Column(name = "humidity_percent")
    /** 습도 */
    private Integer humidityPercent;

    @Column(name = "wind_speed_ms", precision = 5, scale = 1)
    /** 풍속 */
    private BigDecimal windSpeedMs;

    @Column(name = "fetched_at", nullable = false)
    /** 수집시각 */
    private LocalDateTime fetchedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "weather_grid_id", referencedColumnName = "weather_grid_id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "fk_weather_grid_to_weather_forecast"))
    private WeatherGrid weatherGrid;
}
