package com.smarttraveler.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.smarttraveler.admin.service.ReservationRefundService;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** 환불 규정: 체크인 7일 전까지 100% · 3일 전까지 50% · 그 뒤 0% · 체크인 지나면 -1(취소 불가). */
class ReservationRefundRateTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 23);

    @Test
    void refundRateFollowsPolicy() {
        assertEquals(100, ReservationRefundService.refundRate(TODAY, TODAY.plusDays(30)));
        assertEquals(100, ReservationRefundService.refundRate(TODAY, TODAY.plusDays(7)));
        assertEquals(50, ReservationRefundService.refundRate(TODAY, TODAY.plusDays(6)));
        assertEquals(50, ReservationRefundService.refundRate(TODAY, TODAY.plusDays(3)));
        assertEquals(0, ReservationRefundService.refundRate(TODAY, TODAY.plusDays(2)));
        assertEquals(0, ReservationRefundService.refundRate(TODAY, TODAY));
        assertEquals(-1, ReservationRefundService.refundRate(TODAY, TODAY.minusDays(1)));
    }
}
