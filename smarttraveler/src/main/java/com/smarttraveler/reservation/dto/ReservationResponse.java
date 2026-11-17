package com.smarttraveler.reservation.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record ReservationResponse(
        Long reservationId,
        Long roomId,
        LocalDate checkinDate,
        LocalDate checkoutDate,
        Integer guestCount,
        Integer totalPrice,
        String status,
        String reservationNo,
        OffsetDateTime holdExpiresAt
) {
}
