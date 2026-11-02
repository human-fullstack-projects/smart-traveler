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
@Table(name = "legal_dong_map")
@IdClass(LegalDongMapId.class)
/** 법정동·행정동 매핑 */
public class LegalDongMap {
    @Id
    @Column(name = "b_code", nullable = false, length = 10)
    /** 법정동 코드 */
    private String bCode;

    @Id
    @Column(name = "region_id", nullable = false)
    /** 행정동 지역 번호 */
    private Long regionId;

    @Column(name = "b_name", length = 50)
    /** 법정동명 */
    private String bName;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "region_id", referencedColumnName = "region_id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "FK_legal_dong_map_region_id"))
    private Region region;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "b_code", referencedColumnName = "b_code", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "FK_legal_dong_map_b_code"))
    private LegalDong b;
}
