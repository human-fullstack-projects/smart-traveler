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
@Table(name = "region_external_code", uniqueConstraints = {
        @UniqueConstraint(name = "UQ_region_external_provider_code", columnNames = {"provider", "external_code"})
})
/** 지역 외부 코드 */
public class RegionExternalCode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "region_external_code_id", nullable = false)
    /** 외부지역코드ID */
    private Long regionExternalCodeId;

    @Column(name = "provider", nullable = false, length = 30)
    /** 제공처 */
    private String provider;

    @Column(name = "external_code", nullable = false, length = 100)
    /** 외부코드 */
    private String externalCode;

    @Column(name = "region_id")
    /** 지역ID */
    private Long regionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "region_id", referencedColumnName = "region_id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "fk_region_to_region_external_code"))
    private Region region;
}
