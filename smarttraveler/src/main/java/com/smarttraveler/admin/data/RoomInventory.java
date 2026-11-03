package com.smarttraveler.admin.data;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.*;

@Entity(name = "AdminRoomInventory") @Table(name = "room_inventory") @Getter @Setter
public class RoomInventory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "inventory_id") private Long inventoryId;
    @Column(name = "room_id") private Long roomId;
    @Column(name = "stay_date") private LocalDate stayDate;
    private Integer price;
    @Column(name = "price_type", length = 20) private String priceType;
    @Column(name = "is_on_sale") private Boolean isOnSale;
    @Column(name = "total_quantity") private Integer totalQuantity;
    @Column(name = "held_quantity") private Integer heldQuantity;
    @Column(name = "reserved_quantity") private Integer reservedQuantity;
    @Column(name = "updated_at") private LocalDateTime updatedAt;
}

