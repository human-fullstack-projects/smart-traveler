package com.smarttraveler.reservation.dto;

import static com.smarttraveler.mypage.dto.MyPageDtos.COURSE_REFERENCE_PATTERN;

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
        @Pattern(regexp = COURSE_REFERENCE_PATTERN) String courseId
) {}
