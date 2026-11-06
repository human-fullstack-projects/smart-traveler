package com.smarttraveler.payment.service;

import com.smarttraveler.domain.entity.RoomPayment;
import com.smarttraveler.payment.dto.PaymentResponse;
import com.smarttraveler.payment.repository.PaymentRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository paymentRepository;

    public List<PaymentResponse> listByReservation(Long reservationId) {
        return paymentRepository.findByReservationId(reservationId).stream()
                .map(this::toResponse)
                .toList();
    }

    public PaymentResponse get(Long paymentId) {
        RoomPayment payment = paymentRepository.findById(paymentId).get();
        return toResponse(payment);
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
