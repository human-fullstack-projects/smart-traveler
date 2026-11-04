package com.smarttraveler.admin.data;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.*;
import java.math.BigDecimal;

@Entity(name = "AdminRoom") @Table(name = "room") @Getter @Setter
public class Room {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "room_id") private Long roomId;
    @Column(name = "lodging_id") private Long lodgingId;
    @Column(name = "name", length = 100) private String name;
    @Column(name = "base_capacity") private Integer baseCapacity;
    @Column(name = "max_capacity") private Integer maxCapacity;
    @Column(name = "price_off_weekday") private Integer priceOffWeekday;
    @Column(name = "price_off_weekend") private Integer priceOffWeekend;
    @Column(name = "is_active") private Boolean isActive;
    @Column(name = "created_at") private LocalDateTime createdAt;
    @Column(name = "updated_at") private LocalDateTime updatedAt;
}

