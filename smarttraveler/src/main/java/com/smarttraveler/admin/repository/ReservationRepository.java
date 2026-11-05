package com.smarttraveler.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.smarttraveler.domain.entity.RoomReservation;

public interface ReservationRepository extends JpaRepository<RoomReservation, Long> {
    java.util.List<RoomReservation> findByRoomIdIn(java.util.Collection<Long> roomIds);
}

