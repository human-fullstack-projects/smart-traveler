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
@Table(name = "recommendation_place", uniqueConstraints = {
        @UniqueConstraint(name = "UK_place_external_ref", columnNames = {"external_ref"})
})
/** 추천용 장소 (외부 장소와 통합 검토) */
public class RecommendationPlace {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    /** 장소 번호 */
    private Long id;

    @Column(name = "name", nullable = false, length = 200)
    /** 장소 이름 */
    private String name;

    @Column(name = "destination", nullable = false, length = 100)
    /** 목적지 */
    private String destination;

    @Column(name = "area", length = 100)
    /** 세부 지역 */
    private String area;

    @Column(name = "latitude", precision = 9, scale = 6)
    /** 위도 */
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 9, scale = 6)
    /** 경도 */
    private BigDecimal longitude;

    @Column(name = "external_ref", length = 100)
    /** 외부 장소 ID */
    private String externalRef;

    @ColumnDefault("'unverified'")
    @Column(name = "status", nullable = false, columnDefinition = "ENUM('active','closed','unverified')")
    /** 장소 상태 */
    private String status;
}
