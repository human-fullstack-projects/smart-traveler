package com.smarttraveler.reservation.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CreateReservationRequest(
        @NotNull @Positive Long roomId,
        @NotNull LocalDate checkIn,
        @NotNull LocalDate checkOut,
        @Positive int guestCount,
        @Size(max = 50) String visitorName,
        @Size(max = 500) String requestMemo,
        @AssertTrue boolean agreedToPolicy,
        @Positive int expectedTotalPrice,
        @Pattern(regexp = "(course|draft)-[1-9][0-9]{0,17}") String courseId
) {}
