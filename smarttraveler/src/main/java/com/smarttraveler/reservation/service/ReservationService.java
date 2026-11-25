package com.smarttraveler.reservation.service;

import com.smarttraveler.admin.repository.RoomRepository;
import com.smarttraveler.auth.repository.MemberRepository;
import com.smarttraveler.domain.entity.Member;
import com.smarttraveler.domain.entity.RoomReservation;
import com.smarttraveler.mypage.dto.MyPageDtos.CourseView;
import com.smarttraveler.mypage.service.MyPageService;
import com.smarttraveler.reservation.dto.CreateReservationRequest;
import com.smarttraveler.reservation.dto.ReservationItinerary;
import com.smarttraveler.reservation.dto.ReservationItinerary.BookedStay;
import com.smarttraveler.reservation.dto.ReservationResponse;
import com.smarttraveler.reservation.repository.ReservationRepository;
import com.smarttraveler.stay.service.StayInventoryService;
import com.smarttraveler.stay.service.StayService;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class ReservationService {
    private final ReservationRepository reservations;
    private final MemberRepository members;
    private final RoomRepository rooms;
    private final StayService stays;
    private final StayInventoryService inventory;
    private final JdbcTemplate jdbc;
    private final MyPageService myPage;
    private final ObjectMapper mapper;

    @Transactional
    public ReservationResponse create(CreateReservationRequest request, Authentication auth) {
        Member member = member(auth);
        if ("OWNER".equals(member.getRoleCode())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "예약은 일반 사용자 계정으로 이용해주세요.");
        }
        String bookerName = member.getNickname() == null || member.getNickname().isBlank()
                ? member.getLoginId() : member.getNickname().strip();
        if (member.getEmail() == null || member.getEmail().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "내 정보에 이메일을 등록해 주세요.");
        }
        String visitorName = request.visitorName() == null ? bookerName : request.visitorName().strip();
        if (visitorName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "방문자 이름을 입력해 주세요.");
        }
        inventory.validateDates(request.checkIn(), request.checkOut());
        CourseView course = request.courseId() == null ? null
                : myPage.course(member.getMemberId(), request.courseId(), request.checkIn());
        var room = rooms.findById(request.roomId())
                .filter(item -> Boolean.TRUE.equals(item.getIsActive()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        var lodging = stays.requireActive(room.getLodgingId());
        if (request.guestCount() > room.getMaxCapacity()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "객실 정원을 초과했습니다.");
        }

        var nights = inventory.nights(room.getRoomId(), request.checkIn(), request.checkOut(), true);
        if (!inventory.available(nights, request.checkIn(), request.checkOut())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "선택한 날짜에 예약할 수 없습니다.");
        }
        int total = inventory.total(nights);
        if (request.expectedTotalPrice() != total) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "객실 요금이 변경되었습니다.");
        }

        RoomReservation reservation = new RoomReservation();
        reservation.setReservationNo("ST-" + UUID.randomUUID());
        reservation.setMemberId(member.getMemberId());
        reservation.setRoomId(room.getRoomId());
        reservation.setCheckinDate(request.checkIn());
        reservation.setCheckoutDate(request.checkOut());
        reservation.setGuestCount(request.guestCount());
        reservation.setBookerName(bookerName);
        reservation.setBookerPhone("");
        reservation.setBookerEmail(member.getEmail().strip());
        reservation.setVisitorName(visitorName);
        reservation.setRequestMemo(request.requestMemo());
        reservation.setRoomQuantity(1);
        reservation.setStatus("PENDING_PAYMENT");
        reservation.setTotalPrice(total);
        if (course != null) {
            if (course.id().startsWith("course-")) reservation.setCourseId(Long.parseLong(course.id().substring(7)));
            var bookedStay = new BookedStay(lodging.getLodgingId(), lodging.getName(), lodging.getAddress(),
                    lodging.getAddressDetail(), lodging.getLatitude(), lodging.getLongitude(), room.getRoomId(),
                    room.getName(), request.checkIn(), request.checkOut());
            reservation.setItinerarySnapshot(mapper.writeValueAsString(new ReservationItinerary(course, bookedStay)));
        }
        reservations.saveAndFlush(reservation);

        LocalDateTime expiresAt = jdbc.queryForObject("SELECT DATE_ADD(NOW(), INTERVAL 10 MINUTE)",
                (rs, row) -> rs.getObject(1, LocalDateTime.class));
        for (var night : nights) {
            jdbc.update("""
                INSERT INTO reservation_inventory
                    (reservation_id, inventory_id, room_id, quantity, unit_price, allocation_status, hold_expires_at)
                VALUES (?, ?, ?, 1, ?, 'HELD', ?)
                """, reservation.getReservationId(), night.inventoryId(), room.getRoomId(), night.price(), expiresAt);
            jdbc.update("UPDATE room_inventory SET held_quantity=held_quantity+1 WHERE inventory_id=?",
                    night.inventoryId());
        }
        return toResponse(reservation, holdExpiresAt(reservation.getReservationId()));
    }

    public List<ReservationResponse> listByMember(Long memberId, Authentication auth) {
        if (!memberId.equals(memberId(auth))) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return reservations.findByMemberId(memberId).stream()
                .map(reservation -> toResponse(reservation, null))
                .toList();
    }

    public ReservationResponse get(Long reservationId, Authentication auth) {
        Long memberId = memberId(auth);
        var reservation = reservations.findById(reservationId)
                .filter(item -> memberId.equals(item.getMemberId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return toResponse(reservation, holdExpiresAt(reservationId));
    }

    private Long memberId(Authentication auth) {
        return member(auth).getMemberId();
    }

    private Member member(Authentication auth) {
        return members.findByLoginId(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    private OffsetDateTime holdExpiresAt(Long reservationId) {
        return jdbc.queryForObject("""
            SELECT TIMESTAMPDIFF(SECOND, '1970-01-01 00:00:00', UTC_TIMESTAMP())
                   + TIMESTAMPDIFF(SECOND, NOW(), MIN(hold_expires_at)) AS expires_epoch
            FROM reservation_inventory WHERE reservation_id=? AND allocation_status='HELD'
            """, (rs, row) -> {
                Long seconds = rs.getObject(1, Long.class);
                return seconds == null ? null : java.time.Instant.ofEpochSecond(seconds).atOffset(ZoneOffset.UTC);
            }, reservationId);
    }

    private ReservationResponse toResponse(RoomReservation reservation, OffsetDateTime expiresAt) {
        var snapshot = reservation.getItinerarySnapshot();
        return new ReservationResponse(reservation.getReservationId(), reservation.getRoomId(),
                reservation.getCheckinDate(), reservation.getCheckoutDate(), reservation.getGuestCount(),
                reservation.getTotalPrice(), reservation.getStatus(), reservation.getReservationNo(), expiresAt,
                snapshot == null ? null : mapper.readValue(snapshot, ReservationItinerary.class));
    }
}
