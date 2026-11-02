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
@Table(name = "place_external_reference", uniqueConstraints = {
        @UniqueConstraint(name = "UQ_place_external_provider_id", columnNames = {"provider", "external_place_id"})
})
/** 장소 외부 제공처 매핑 */
public class PlaceExternalReference {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "place_external_ref_id", nullable = false)
    /** 외부 매핑 번호 */
    private Long placeExternalRefId;

    @Column(name = "place_id", nullable = false)
    /** 공통 장소 번호 */
    private Long placeId;

    @Column(name = "provider", nullable = false, length = 30)
    /** 외부 제공처 */
    private String provider;

    @Column(name = "external_place_id", nullable = false, length = 100)
    /** 외부 장소 ID */
    private String externalPlaceId;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at", nullable = false)
    /** 생성일시 */
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "place_id", referencedColumnName = "place_id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "FK_place_external_reference_place_id"))
    private Place place;
}
