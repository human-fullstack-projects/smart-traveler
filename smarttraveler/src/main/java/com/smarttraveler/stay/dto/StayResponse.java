package com.smarttraveler.stay.dto;

import java.math.BigDecimal;
import java.time.LocalTime;

public record StayResponse(
        Long lodgingId,
        String name,
        String address,
        String lodgingType,
        BigDecimal latitude,
        BigDecimal longitude,
        LocalTime checkinTime,
        LocalTime checkoutTime
) {
}
