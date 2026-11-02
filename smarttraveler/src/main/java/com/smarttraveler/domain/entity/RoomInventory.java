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
@Table(name = "room_inventory", uniqueConstraints = {
        @UniqueConstraint(name = "UQ_room_inventory_room_id_stay_date", columnNames = {"room_id", "stay_date"}),
        @UniqueConstraint(name = "UQ_inventory_id_room", columnNames = {"inventory_id", "room_id"})
})
/** 객실 재고 */
public class RoomInventory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inventory_id", nullable = false)
    /** 재고 ID */
    private Long inventoryId;

    @Column(name = "room_id", nullable = false)
    /** 객실 ID */
    private Long roomId;

    @Column(name = "stay_date", nullable = false)
    /** 숙박 날짜 */
    private LocalDate stayDate;

    @Column(name = "price", nullable = false)
    /** 그날 요금 */
    private Integer price;

    @ColumnDefault("'OWNER_SET'")
    @Column(name = "price_type", nullable = false, length = 20)
    /** 요금 구분 */
    private String priceType;

    @ColumnDefault("TRUE")
    @Column(name = "is_on_sale", nullable = false)
    /** 판매 여부 */
    private Boolean isOnSale;

    @ColumnDefault("CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    @Column(name = "updated_at", nullable = false)
    /** 수정일시 */
    private LocalDateTime updatedAt;

    @ColumnDefault("1")
    @Column(name = "total_quantity", nullable = false)
    /** 판매 객실 총수량 */
    private Integer totalQuantity;

    @ColumnDefault("0")
    @Column(name = "held_quantity", nullable = false)
    /** 결제 대기 점유수량 */
    private Integer heldQuantity;

    @ColumnDefault("0")
    @Column(name = "reserved_quantity", nullable = false)
    /** 예약 확정수량 */
    private Integer reservedQuantity;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", referencedColumnName = "room_id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "FK_room_TO_room_inventory_room_id"))
    private Room room;
}
