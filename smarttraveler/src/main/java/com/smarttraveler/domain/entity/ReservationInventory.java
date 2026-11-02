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
@Table(name = "reservation_inventory", uniqueConstraints = {
        @UniqueConstraint(name = "UQ_allocation_res_inventory", columnNames = {"reservation_id", "inventory_id"})
})
/** 예약별 일자 재고 할당 */
public class ReservationInventory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "allocation_id", nullable = false)
    /** 할당 번호 */
    private Long allocationId;

    @Column(name = "reservation_id", nullable = false)
    /** 객실 예약 번호 */
    private Long reservationId;

    @Column(name = "inventory_id", nullable = false)
    /** 날짜별 재고 번호 */
    private Long inventoryId;

    @Column(name = "room_id", nullable = false)
    /** 객실 유형 번호 */
    private Long roomId;

    @Column(name = "quantity", nullable = false)
    /** 예약 수량 */
    private Integer quantity;

    @Column(name = "unit_price", nullable = false)
    /** 예약 당시 1실 요금 */
    private Integer unitPrice;

    @ColumnDefault("'HELD'")
    @Column(name = "allocation_status", nullable = false, length = 20)
    /** 할당 상태 */
    private String allocationStatus;

    @Column(name = "hold_expires_at")
    /** 점유 만료 시각 */
    private LocalDateTime holdExpiresAt;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at", nullable = false)
    /** 생성일시 */
    private LocalDateTime createdAt;

    @ColumnDefault("CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    @Column(name = "updated_at", nullable = false)
    /** 변경 일시 */
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumns(
            value = {
                    @JoinColumn(name = "reservation_id", referencedColumnName = "reservation_id", insertable = false, updatable = false),
                    @JoinColumn(name = "room_id", referencedColumnName = "room_id", insertable = false, updatable = false)
            },
            foreignKey = @ForeignKey(name = "FK_allocation_reservation_room")
    )
    private RoomReservation reservationAndRoom;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumns(
            value = {
                    @JoinColumn(name = "inventory_id", referencedColumnName = "inventory_id", insertable = false, updatable = false),
                    @JoinColumn(name = "room_id", referencedColumnName = "room_id", insertable = false, updatable = false)
            },
            foreignKey = @ForeignKey(name = "FK_allocation_inventory_room")
    )
    private RoomInventory inventoryAndRoom;
}
