package com.smarttraveler.admin.repository;

import com.smarttraveler.domain.entity.ReservationActionHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReservationActionHistoryRepository extends JpaRepository<ReservationActionHistory, Long> {
    List<ReservationActionHistory> findByReservationIdOrderByCreatedAtDesc(Long reservationId);
}
