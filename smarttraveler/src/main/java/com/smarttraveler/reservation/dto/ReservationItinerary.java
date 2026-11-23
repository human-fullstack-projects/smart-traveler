package com.smarttraveler.reservation.dto;

import com.smarttraveler.mypage.dto.MyPageDtos.CourseView;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ReservationItinerary(CourseView course, BookedStay stay) {
    public record BookedStay(Long lodgingId, String name, String address, String addressDetail,
                             BigDecimal latitude, BigDecimal longitude, Long roomId, String roomName,
                             LocalDate checkIn, LocalDate checkOut) {}
}
