package com.smarttraveler.reservation.repository;

import com.smarttraveler.domain.entity.RoomReservation;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository("reservationApiReservationRepository")
public interface ReservationRepository extends JpaRepository<RoomReservation, Long> {
    List<RoomReservation> findByMemberId(Long memberId);
}
