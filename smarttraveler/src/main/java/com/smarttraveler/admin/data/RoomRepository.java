package com.smarttraveler.admin.data;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepository extends JpaRepository<Room, Long> {
    java.util.List<Room> findByLodgingId(Long lodgingId);
}

