package com.smarttraveler.payment.repository;

import com.smarttraveler.domain.entity.RoomPayment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository("paymentApiPaymentRepository")
public interface PaymentRepository extends JpaRepository<RoomPayment, Long> {
    List<RoomPayment> findByReservationId(Long reservationId);
    Optional<RoomPayment> findFirstByReservationIdOrderByCreatedAtDesc(Long reservationId);
    Optional<RoomPayment> findByOrderId(String orderId);
}
