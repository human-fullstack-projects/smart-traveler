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
@Table(name = "weather_grid")
/** 기상 격자 */
public class WeatherGrid {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "weather_grid_id", nullable = false)
    /** 기상격자ID */
    private Long weatherGridId;

    @Column(name = "provider", nullable = false, length = 30)
    /** 제공처 */
    private String provider;

    @Column(name = "grid_x", nullable = false)
    /** 격자X */
    private Integer gridX;

    @Column(name = "grid_y", nullable = false)
    /** 격자Y */
    private Integer gridY;

    @Column(name = "latitude", precision = 10, scale = 7)
    /** 위도 */
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 10, scale = 7)
    /** 경도 */
    private BigDecimal longitude;
}
