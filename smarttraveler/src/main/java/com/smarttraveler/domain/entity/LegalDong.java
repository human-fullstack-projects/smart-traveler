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
@Table(name = "legal_dong", uniqueConstraints = {
        @UniqueConstraint(name = "UQ_legal_dong_b_code", columnNames = {"b_code"})
})
/** 법정동 지역 */
public class LegalDong {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "legal_dong_id", nullable = false)
    /** 법정동 지역 번호 */
    private Long legalDongId;

    @Column(name = "b_code", nullable = false, length = 10)
    /** 법정동 코드 */
    private String bCode;

    @Column(name = "parent_b_code", length = 10)
    /** 상위 법정동 코드 */
    private String parentBCode;

    @Column(name = "parent_legal_dong_id")
    /** 상위 법정동 */
    private Long parentLegalDongId;

    @Column(name = "region_level", nullable = false)
    /** 지역 단계 1~4 */
    private Integer regionLevel;

    @Column(name = "region_name", nullable = false, length = 50)
    /** 지역명 */
    private String regionName;

    @Column(name = "full_name", nullable = false, length = 150)
    /** 전체 지역명 */
    private String fullName;

    @Column(name = "sido_cd", nullable = false, length = 2)
    /** 시도 코드 */
    private String sidoCd;

    @Column(name = "sgg_cd", nullable = false, length = 3)
    /** 시군구 코드 */
    private String sggCd;

    @Column(name = "umd_cd", nullable = false, length = 3)
    /** 읍면동 코드 */
    private String umdCd;

    @Column(name = "ri_cd", nullable = false, length = 2)
    /** 리 코드 */
    private String riCd;

    @Column(name = "center_lat", precision = 10, scale = 7)
    /** 중심 위도 */
    private BigDecimal centerLat;

    @Column(name = "center_lng", precision = 10, scale = 7)
    /** 중심 경도 */
    private BigDecimal centerLng;

    @Column(name = "kma_nx")
    /** 기상 격자 X */
    private Short kmaNx;

    @Column(name = "kma_ny")
    /** 기상 격자 Y */
    private Short kmaNy;

    @Column(name = "adopted_at")
    /** 시행일 */
    private LocalDate adoptedAt;

    @ColumnDefault("TRUE")
    @Column(name = "use_yn", nullable = false)
    /** 사용 여부 */
    private Boolean useYn;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at", nullable = false)
    /** 생성일시 */
    private LocalDateTime createdAt;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "updated_at", nullable = false)
    /** 수정일시 */
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "parent_legal_dong_id", referencedColumnName = "legal_dong_id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "FK_legal_dong_parent_legal_dong_id"))
    private LegalDong parentLegalDong;
}
