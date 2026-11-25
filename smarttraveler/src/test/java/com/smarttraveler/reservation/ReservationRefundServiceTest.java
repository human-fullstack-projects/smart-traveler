package com.smarttraveler.reservation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.smarttraveler.admin.service.ReservationRefundService;
import com.smarttraveler.payment.service.TossGateway;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class ReservationRefundServiceTest {
    private static final String MEMBER = "traveler";
    private static final String MEMBER_REASON = "회원 요청으로 예약 취소";
    private static final String PAYMENT_KEY = "payment-test";
    private static final long MEMBER_ID = 7L;
    private static final long RESERVATION_ID = 42L;
    private static final long PAYMENT_ID = 81L;
    private static final int PAID = 50_000;
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final TossGateway toss = mock(TossGateway.class);
    private final ObjectMapper mapper = new ObjectMapper();
    private final ReservationRefundService service;

    ReservationRefundServiceTest() {
        var manager = mock(PlatformTransactionManager.class);
        when(manager.getTransaction(any())).thenAnswer(call -> new SimpleTransactionStatus());
        service = new ReservationRefundService(jdbc, toss, manager, mapper);
        when(jdbc.queryForList(contains("FROM members"), eq(Long.class), eq(MEMBER))).thenReturn(List.of(MEMBER_ID));
    }

    @Test
    void memberReceivesFullRefundEvenOneDayBeforeCheckIn() {
        memberReservation("CONFIRMED", 1);
        payment(PAID, 0);
        startRefund("CONFIRMED", 1);
        when(toss.get(PAYMENT_KEY)).thenReturn(mapper.readTree("{\"status\":\"DONE\"}"));
        when(toss.cancel(PAYMENT_KEY, MEMBER_REASON, null, "member-refund-" + PAYMENT_ID))
                .thenReturn(tossCancellation("CANCELED", PAID));
        completeRefund(2);

        assertEquals("CANCELLED", service.cancelByMember(RESERVATION_ID, MEMBER));

        verify(jdbc).update(contains("SET refund_amount=?"), eq(PAID), eq(MEMBER_REASON), eq(PAYMENT_ID));
        verify(jdbc).update(contains("SET i.reserved_quantity"), eq(RESERVATION_ID));
        verify(jdbc).update(contains("SET allocation_status='CANCELLED'"), eq(RESERVATION_ID));
    }

    @Test
    void onlyLocksTheSignedInMembersReservation() {
        when(jdbc.queryForList(contains("member_id=COALESCE"), eq(RESERVATION_ID), eq(MEMBER_ID)))
                .thenReturn(List.of());

        var error = assertThrows(ResponseStatusException.class, () -> service.cancelByMember(RESERVATION_ID, MEMBER));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        verifyNoInteractions(toss);
        verify(jdbc, never()).queryForList(contains("FROM room_payment"), eq(RESERVATION_ID));
    }

    @Test
    void cancelledReservationCannotTriggerAnotherRefund() {
        memberReservation("CANCELLED", 3);

        assertThrows(IllegalStateException.class, () -> service.cancelByMember(RESERVATION_ID, MEMBER));

        verifyNoInteractions(toss);
    }

    @Test
    void preservesInventoryOnGatewayFailureAndRecoversARefundThatTossAlreadyProcessed() {
        memberReservation("CONFIRMED", 1);
        payment(PAID, 0);
        startRefund("CONFIRMED", 1);
        when(toss.get(PAYMENT_KEY)).thenReturn(mapper.readTree("{\"status\":\"DONE\"}"));
        when(toss.cancel(PAYMENT_KEY, MEMBER_REASON, null, "member-refund-" + PAYMENT_ID))
                .thenThrow(new ResourceAccessException("response lost"));
        when(jdbc.update(contains("SET status='REFUND_FAILED'"), eq(RESERVATION_ID), eq(2))).thenReturn(1);

        assertEquals("REFUND_FAILED", service.cancelByMember(RESERVATION_ID, MEMBER));
        verify(jdbc, never()).update(contains("SET i.reserved_quantity"), eq(RESERVATION_ID));

        memberReservation("REFUND_FAILED", 3);
        payment(PAID, PAID);
        startRefund("REFUND_FAILED", 3);
        when(toss.get(PAYMENT_KEY)).thenReturn(tossCancellation("CANCELED", PAID));
        completeRefund(4);

        assertEquals("CANCELLED", service.cancelByMember(RESERVATION_ID, MEMBER));
        verify(toss).cancel(PAYMENT_KEY, MEMBER_REASON, null, "member-refund-" + PAYMENT_ID);
        verify(jdbc).update(contains("SET i.reserved_quantity"), eq(RESERVATION_ID));
    }

    @Test
    void doesNotReplaceAnExistingAdministratorPartialRefund() {
        memberReservation("REFUND_FAILED", 3);
        payment(PAID, PAID / 2);

        assertThrows(IllegalStateException.class, () -> service.cancelByMember(RESERVATION_ID, MEMBER));

        verifyNoInteractions(toss);
    }

    @Test
    void administratorStillUsesTheExistingRate() {
        when(jdbc.queryForList(contains("FROM members"), eq(Long.class), eq("admin"))).thenReturn(List.of(3L));
        when(jdbc.queryForList(contains("FOR UPDATE"), eq(RESERVATION_ID), isNull())).thenReturn(List.of(Map.of(
                "status", "CONFIRMED", "version", 1, "checkin_date", LocalDate.now().plusDays(3))));
        payment(PAID, 0);
        startRefund("CONFIRMED", 1);
        when(toss.get(PAYMENT_KEY)).thenReturn(mapper.readTree("{\"status\":\"DONE\"}"));
        when(toss.cancel(eq(PAYMENT_KEY), eq("관리자 취소"), eq(PAID / 2), anyString()))
                .thenReturn(tossCancellation("PARTIAL_CANCELED", PAID / 2));
        when(jdbc.update(contains("SET status='CANCELLED'"), eq("관리자 취소"), eq(RESERVATION_ID), eq(2))).thenReturn(1);

        assertEquals("CANCELLED", service.cancelAndRefund(RESERVATION_ID, "관리자 취소", "admin"));

        verify(toss).cancel(eq(PAYMENT_KEY), eq("관리자 취소"), eq(PAID / 2), anyString());
    }

    private void memberReservation(String status, int version) {
        var checkIn = LocalDate.now(ZoneId.of("Asia/Seoul")).plusDays(1);
        when(jdbc.queryForList(contains("FOR UPDATE"), eq(RESERVATION_ID), eq(MEMBER_ID)))
                .thenReturn(List.of(Map.of("status", status, "version", version, "checkin_date", checkIn)));
    }

    private void payment(int amount, int planned) {
        when(jdbc.queryForList(contains("FROM room_payment"), eq(RESERVATION_ID))).thenReturn(List.of(
                Map.of("payment_id", PAYMENT_ID, "payment_key", PAYMENT_KEY, "amount", amount, "refund_amount", planned)));
    }

    private void startRefund(String status, int version) {
        when(jdbc.update(contains("SET status='REFUNDING'"), eq(RESERVATION_ID), eq(status), eq(version))).thenReturn(1);
    }

    private void completeRefund(int version) {
        when(jdbc.update(contains("SET status='CANCELLED'"), eq(MEMBER_REASON), eq(RESERVATION_ID), eq(version))).thenReturn(1);
        when(jdbc.update(contains("SET i.reserved_quantity"), eq(RESERVATION_ID))).thenReturn(2);
        when(jdbc.update(contains("SET allocation_status='CANCELLED'"), eq(RESERVATION_ID))).thenReturn(2);
    }

    private JsonNode tossCancellation(String status, int amount) {
        return mapper.readTree("""
                {"status":"%s","cancels":[{"cancelStatus":"DONE","cancelAmount":%d}]}
                """.formatted(status, amount));
    }
}
