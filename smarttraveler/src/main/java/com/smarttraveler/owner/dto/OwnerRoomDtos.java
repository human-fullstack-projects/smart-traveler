package com.smarttraveler.owner.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** 업주 객실·판매 수량·예약 화면에서 주고받는 값. */
public final class OwnerRoomDtos {
    private OwnerRoomDtos() {}

    /** 객실 입력 (숙소 등록 때 · 객실 추가 때). quantity = 하루에 팔 객실 수. */
    public record RoomInput(
            @NotBlank @Size(max = 100) String name,
            @Min(1) @Max(30) int baseCapacity,
            @Min(1) @Max(30) int maxCapacity,
            @Min(1000) @Max(10000000) int priceWeekday,
            @Min(1000) @Max(10000000) int priceWeekend,
            @Min(0) @Max(500) int quantity
    ) {}

    public record OwnerRoom(long roomId, long lodgingId, String lodgingName, String name, int baseCapacity, int maxCapacity,
                            int priceWeekday, int priceWeekend, boolean active, Integer todayTotal, Integer todayRemaining) {}

    public record InventoryDay(LocalDate date, int total, int used, int remaining, boolean onSale, int price) {}

    public record InventoryUpdate(@NotNull LocalDate from, @NotNull LocalDate to, @Min(0) @Max(500) int quantity) {}

    public record OwnerReservation(long reservationId, String reservationNo, String bookerName, String bookerPhone,
                                   int guestCount, int roomQuantity, LocalDate checkIn, LocalDate checkOut, int totalPrice,
                                   String status, String requestMemo, String roomName, String lodgingName,
                                   String bookerEmail, String visitorName) {}
}
