package com.smarttraveler.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.smarttraveler.domain.entity.RoomPayment;

public interface PaymentRepository extends JpaRepository<RoomPayment, Long> {
    java.util.List<RoomPayment> findByReservationIdOrderByCreatedAtDesc(Long reservationId);
}

