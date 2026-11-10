package com.smarttraveler.payment.service;

import com.smarttraveler.domain.entity.RoomPayment;
import com.smarttraveler.payment.dto.PaymentResponse;
import com.smarttraveler.payment.repository.PaymentRepository;
import com.smarttraveler.reservation.repository.ReservationRepository;
import com.smarttraveler.auth.repository.MemberRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final ReservationRepository reservations;
    private final MemberRepository members;

    public List<PaymentResponse> listByReservation(Long reservationId, Authentication auth) {
        requireOwner(reservationId, auth);
        return paymentRepository.findByReservationId(reservationId).stream()
                .map(this::toResponse)
                .toList();
    }

    public PaymentResponse get(Long paymentId, Authentication auth) {
        RoomPayment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        requireOwner(payment.getReservationId(), auth);
        return toResponse(payment);
    }

    private void requireOwner(Long reservationId, Authentication auth) {
        Long memberId = members.findByLoginId(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED)).getMemberId();
        var reservation = reservations.findById(reservationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!memberId.equals(reservation.getMemberId())) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }

    private PaymentResponse toResponse(RoomPayment payment) {
        return new PaymentResponse(
                payment.getPaymentId(),
                payment.getReservationId(),
                payment.getMethod(),
                payment.getAmount(),
                payment.getStatus()
        );
    }
}
