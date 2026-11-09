package com.smarttraveler.owner.dto;

import java.math.BigDecimal;
import java.time.LocalTime;

public record OwnerLodgingDetail(
        Long lodgingId,
        String name,
        String lodgingType,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        LocalTime checkinTime,
        LocalTime checkoutTime,
        String status,
        String rejectReason
) {}