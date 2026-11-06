package com.smarttraveler.reservation.service;

import com.smarttraveler.domain.entity.RoomReservation;
import com.smarttraveler.reservation.dto.ReservationResponse;
import com.smarttraveler.reservation.repository.ReservationRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReservationService {
    private final ReservationRepository reservationRepository;

    public List<ReservationResponse> listByMember(Long memberId) {
        return reservationRepository.findByMemberId(memberId).stream()
                .map(this::toResponse)
                .toList();
    }

    public ReservationResponse get(Long reservationId) {
        RoomReservation reservation = reservationRepository.findById(reservationId).get();
        return toResponse(reservation);
    }

    private ReservationResponse toResponse(RoomReservation reservation) {
        return new ReservationResponse(
                reservation.getReservationId(),
                reservation.getRoomId(),
                reservation.getCheckinDate(),
                reservation.getCheckoutDate(),
                reservation.getGuestCount(),
                reservation.getTotalPrice(),
                reservation.getStatus()
        );
    }
}
