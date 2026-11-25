package com.smarttraveler.reservation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.smarttraveler.admin.repository.RoomRepository;
import com.smarttraveler.auth.repository.MemberRepository;
import com.smarttraveler.domain.entity.Lodging;
import com.smarttraveler.domain.entity.Member;
import com.smarttraveler.domain.entity.Room;
import com.smarttraveler.domain.entity.RoomPayment;
import com.smarttraveler.domain.entity.RoomReservation;
import com.smarttraveler.mypage.dto.MyPageDtos.CourseView;
import com.smarttraveler.mypage.dto.MyPageDtos.Day;
import com.smarttraveler.mypage.service.MyPageService;
import com.smarttraveler.payment.repository.PaymentRepository;
import com.smarttraveler.payment.service.TossGateway;
import com.smarttraveler.payment.service.TossPaymentService;
import com.smarttraveler.payment.service.TossWebhookVerifier;
import com.smarttraveler.reservation.dto.CreateReservationRequest;
import com.smarttraveler.reservation.repository.ReservationRepository;
import com.smarttraveler.reservation.service.ReservationService;
import com.smarttraveler.stay.service.StayInventoryService;
import com.smarttraveler.stay.service.StayInventoryService.RoomNight;
import com.smarttraveler.stay.service.StayService;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

class ReservationServiceTest {
    private static final LocalDate CHECK_IN = LocalDate.of(2026, 12, 1);
    private static final LocalDate CHECK_OUT = CHECK_IN.plusDays(1);
    private final ReservationRepository reservations = mock(ReservationRepository.class);
    private final MemberRepository members = mock(MemberRepository.class);
    private final RoomRepository rooms = mock(RoomRepository.class);
    private final StayInventoryService inventory = mock(StayInventoryService.class);
    private final StayService stays = mock(StayService.class);
    private final MyPageService myPage = mock(MyPageService.class);
    private final ObjectMapper mapper = new ObjectMapper();
    private final ReservationService service = new ReservationService(reservations, members, rooms, stays,
            inventory, mock(JdbcTemplate.class), myPage, mapper);
    private final Authentication auth = mock(Authentication.class);

    @Test
    void ownerCannotCreateReservationOrHoldInventory() {
        when(auth.getName()).thenReturn("owner");
        var owner = new Member();
        owner.setRoleCode("OWNER");
        when(members.findByLoginId("owner")).thenReturn(Optional.of(owner));

        var error = assertThrows(ResponseStatusException.class, () -> service.create(request(null, null), auth));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        org.mockito.Mockito.verifyNoInteractions(reservations, inventory, stays, rooms);
    }

    @Test
    void ownerCannotPreparePaymentEvenForAnExistingReservation() {
        when(auth.getName()).thenReturn("owner");
        var owner = new Member();
        owner.setRoleCode("OWNER");
        when(members.findByLoginId("owner")).thenReturn(Optional.of(owner));
        var jdbc = mock(JdbcTemplate.class);
        var payments = mock(PaymentRepository.class);
        var gateway = mock(TossGateway.class);
        var paymentService = new TossPaymentService(payments, reservations, members, jdbc, gateway,
                mock(TossWebhookVerifier.class), mapper, "test_gck_local");

        var error = assertThrows(ResponseStatusException.class, () -> paymentService.prepare(77L, auth));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        org.mockito.Mockito.verifyNoInteractions(jdbc, payments, reservations, gateway);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = " 실제 방문자 ")
    void savesBookerFromTheAccountAndVisitorFromTheRequest(String visitorName) {
        setUpReservation();
        var response = service.create(request(visitorName, null), auth);
        var saved = savedReservation();
        assertEquals(7L, saved.getMemberId());
        assertEquals("여행자", saved.getBookerName());
        assertEquals("account@example.invalid", saved.getBookerEmail());
        assertEquals(visitorName == null ? "여행자" : "실제 방문자", saved.getVisitorName());
        assertNull(response.itinerary());
    }

    @ParameterizedTest
    @CsvSource({"course-23,23", "draft-23,", "human-23,"})
    void savesSelectedRouteAndBookedStayTogether(String sourceId, Long linkedCourseId) {
        setUpReservation();
        var course = new CourseView(sourceId, "부산 여행", "부산", CHECK_IN, CHECK_OUT, "DRAFT",
                List.of(new Day(CHECK_IN, List.of("광안리", "민락수변공원"))), null);
        when(myPage.course(7L, sourceId, CHECK_IN)).thenReturn(course);

        var response = service.create(request(null, sourceId), auth);
        var saved = savedReservation();
        assertEquals(linkedCourseId, saved.getCourseId());
        assertEquals(course, response.itinerary().course());
        assertEquals("벡스코호스텔", response.itinerary().stay().name());
        assertEquals("더블룸", response.itinerary().stay().roomName());
        assertEquals(CHECK_OUT, response.itinerary().stay().checkOut());
        verify(myPage).course(7L, sourceId, CHECK_IN);
    }

    @ParameterizedTest
    @ValueSource(strings = {"course-99", "human-99"})
    void rejectsUnavailableCourseBeforeHoldingInventory(String sourceId) {
        setUpReservation();
        when(myPage.course(7L, sourceId, CHECK_IN)).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));
        var error = assertThrows(ResponseStatusException.class, () -> service.create(request(null, sourceId), auth));
        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        verify(reservations, never()).saveAndFlush(any());
        verify(inventory, never()).nights(11L, CHECK_IN, CHECK_OUT, true);
    }

    @ParameterizedTest
    @CsvSource({"DONE,CONFIRMED", "ABORTED,PENDING_PAYMENT"})
    void keepsTheItineraryAndConfirmsOnlyAnApprovedPayment(String paymentStatus, String reservationStatus) {
        setUpReservation();
        var course = new CourseView("course-23", "부산 여행", "부산", CHECK_IN, CHECK_OUT, "DRAFT",
                List.of(new Day(CHECK_IN, List.of("광안리"))), null);
        when(myPage.course(7L, course.id(), CHECK_IN)).thenReturn(course);
        var created = service.create(request(null, course.id()), auth);
        var reservation = savedReservation();
        reservation.setReservationId(77L);
        when(reservations.findById(77L)).thenReturn(Optional.of(reservation));

        var payments = mock(PaymentRepository.class);
        var payment = new RoomPayment();
        payment.setOrderId("order-test");
        payment.setPaymentKey("payment-test");
        payment.setAmount(100_000);
        when(payments.findFirstByReservationIdOrderByCreatedAtDesc(77L)).thenReturn(Optional.of(payment));
        var jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForList(contains("FOR UPDATE"), eq(Long.class), eq(77L))).thenReturn(List.of(77L));
        boolean approved = "DONE".equals(paymentStatus);
        if (approved) {
            when(jdbc.queryForObject(contains("COUNT(*)"), eq(Long.class), eq(77L), eq(11L),
                    eq(CHECK_IN), eq(CHECK_OUT), eq(1), eq(false))).thenReturn(1L);
            when(jdbc.update(contains("SET i.held_quantity"), eq(77L))).thenReturn(1);
        }
        var gateway = mock(TossGateway.class);
        when(gateway.get("payment-test")).thenReturn(mapper.readTree("""
            {"orderId":"order-test","paymentKey":"payment-test","totalAmount":100000,"status":"%s"}
            """.formatted(paymentStatus)));
        var paymentService = new TossPaymentService(payments, reservations, members, jdbc, gateway,
                mock(TossWebhookVerifier.class), mapper, "test_gck_local");

        assertEquals(paymentStatus, paymentService.sync(77L, auth).status());

        if (approved) {
            verify(reservations).save(reservation);
        } else {
            verify(reservations, never()).save(any());
            verify(jdbc, never()).update(contains("SET i.held_quantity"), eq(77L));
        }
        var fetched = service.get(77L, auth);
        assertEquals(reservationStatus, fetched.status());
        assertEquals(created.itinerary(), fetched.itinerary());
    }

    private void setUpReservation() {
        when(auth.getName()).thenReturn("account");
        var member = new Member();
        member.setMemberId(7L);
        member.setNickname("여행자");
        member.setEmail("account@example.invalid");
        when(members.findByLoginId("account")).thenReturn(Optional.of(member));

        var room = new Room();
        room.setRoomId(11L);
        room.setLodgingId(12L);
        room.setName("더블룸");
        room.setIsActive(true);
        room.setMaxCapacity(2);
        when(rooms.findById(11L)).thenReturn(Optional.of(room));
        var lodging = new Lodging();
        lodging.setLodgingId(12L);
        lodging.setName("벡스코호스텔");
        when(stays.requireActive(12L)).thenReturn(lodging);
        var nights = List.of(new RoomNight(13L, CHECK_IN, 100_000, 1, true));
        when(inventory.nights(11L, CHECK_IN, CHECK_OUT, true)).thenReturn(nights);
        when(inventory.available(nights, CHECK_IN, CHECK_OUT)).thenReturn(true);
        when(inventory.total(nights)).thenReturn(100_000);
    }

    private CreateReservationRequest request(String visitorName, String courseId) {
        return new CreateReservationRequest(11L, CHECK_IN, CHECK_OUT, 2, visitorName, null, true, 100_000, courseId);
    }

    private RoomReservation savedReservation() {
        var saved = ArgumentCaptor.forClass(RoomReservation.class);
        verify(reservations).saveAndFlush(saved.capture());
        return saved.getValue();
    }
}
