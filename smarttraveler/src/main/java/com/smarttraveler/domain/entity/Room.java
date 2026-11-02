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
@Table(name = "room", uniqueConstraints = {
        @UniqueConstraint(name = "UQ_room_lodging_id_name", columnNames = {"lodging_id", "name"})
})
/** 객실 */
public class Room {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_id", nullable = false)
    /** 객실 ID */
    private Long roomId;

    @Column(name = "lodging_id", nullable = false)
    /** 숙소 ID */
    private Long lodgingId;

    @Column(name = "name", nullable = false, length = 100)
    /** 객실명 */
    private String name;

    @Column(name = "source_room_code", length = 50)
    /** 원본 객실 코드 */
    private String sourceRoomCode;

    @Column(name = "base_capacity", nullable = false)
    /** 기준 인원 */
    private Integer baseCapacity;

    @Column(name = "max_capacity", nullable = false)
    /** 최대 인원 */
    private Integer maxCapacity;

    @Column(name = "size_m2", precision = 6, scale = 2)
    /** 면적 (㎡) */
    private BigDecimal sizeM2;

    @Column(name = "bed_type", length = 50)
    /** 침대 구성 */
    private String bedType;

    @Column(name = "price_off_weekday", nullable = false)
    /** 비수기 주중 요금 */
    private Integer priceOffWeekday;

    @Column(name = "price_off_weekend", nullable = false)
    /** 비수기 주말 요금 */
    private Integer priceOffWeekend;

    @Column(name = "price_peak_weekday")
    /** 성수기 주중 요금 */
    private Integer pricePeakWeekday;

    @Column(name = "price_peak_weekend")
    /** 성수기 주말 요금 */
    private Integer pricePeakWeekend;

    @Column(name = "amenities", length = 500)
    /** 객실 비품 */
    private String amenities;

    @Column(name = "description", columnDefinition = "TEXT")
    /** 객실 소개 */
    private String description;

    @ColumnDefault("TRUE")
    @Column(name = "is_active", nullable = false)
    /** 판매 중 여부 */
    private Boolean isActive;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at", nullable = false)
    /** 생성일시 */
    private LocalDateTime createdAt;

    @ColumnDefault("CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    @Column(name = "updated_at", nullable = false)
    /** 수정일시 */
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lodging_id", referencedColumnName = "lodging_id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "FK_lodging_TO_room_lodging_id"))
    private Lodging lodging;
}
