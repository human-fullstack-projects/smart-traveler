package com.smarttraveler.admin.data;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    java.util.List<Payment> findByReservationIdOrderByCreatedAtDesc(Long reservationId);
}

