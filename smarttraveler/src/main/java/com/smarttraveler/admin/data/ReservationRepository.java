package com.smarttraveler.admin.data;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    java.util.List<Reservation> findByRoomIdIn(java.util.Collection<Long> roomIds);
}

