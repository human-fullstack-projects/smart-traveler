package com.smarttraveler.reservation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.smarttraveler.admin.repository.RoomRepository;
import com.smarttraveler.auth.repository.MemberRepository;
import com.smarttraveler.domain.entity.Member;
import com.smarttraveler.domain.entity.Room;
import com.smarttraveler.domain.entity.RoomReservation;
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
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;

class ReservationServiceTest {
    @ParameterizedTest
    @NullSource
    @ValueSource(strings = " 실제 방문자 ")
    void savesBookerFromTheAccountAndVisitorFromTheRequest(String visitorName) {
        var reservations = mock(ReservationRepository.class);
        var members = mock(MemberRepository.class);
        var rooms = mock(RoomRepository.class);
        var inventory = mock(StayInventoryService.class);
        var service = new ReservationService(reservations, members, rooms, mock(StayService.class),
                inventory, mock(JdbcTemplate.class));
        var auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("account");
        var member = new Member();
        member.setMemberId(7L);
        member.setNickname("여행자");
        member.setEmail("account@example.invalid");
        when(members.findByLoginId("account")).thenReturn(Optional.of(member));

        var room = new Room();
        room.setRoomId(11L);
        room.setLodgingId(12L);
        room.setIsActive(true);
        room.setMaxCapacity(2);
        when(rooms.findById(11L)).thenReturn(Optional.of(room));
        LocalDate checkIn = LocalDate.of(2026, 12, 1);
        LocalDate checkOut = checkIn.plusDays(1);
        var nights = List.of(new RoomNight(13L, checkIn, 100_000, 1, true));
        when(inventory.nights(11L, checkIn, checkOut, true)).thenReturn(nights);
        when(inventory.available(nights, checkIn, checkOut)).thenReturn(true);
        when(inventory.total(nights)).thenReturn(100_000);

        service.create(new CreateReservationRequest(11L, checkIn, checkOut, 2, visitorName, null, true, 100_000), auth);

        var saved = ArgumentCaptor.forClass(RoomReservation.class);
        verify(reservations).saveAndFlush(saved.capture());
        assertEquals(7L, saved.getValue().getMemberId());
        assertEquals("여행자", saved.getValue().getBookerName());
        assertEquals("account@example.invalid", saved.getValue().getBookerEmail());
        assertEquals(visitorName == null ? "여행자" : "실제 방문자", saved.getValue().getVisitorName());
    }
}
