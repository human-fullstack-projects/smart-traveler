package com.smarttraveler.payment.repository;

import com.smarttraveler.domain.entity.RoomPayment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<RoomPayment, Long> {
    List<RoomPayment> findByReservationId(Long reservationId);
}
