package com.smarttraveler.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.smarttraveler.domain.entity.Room;

public interface RoomRepository extends JpaRepository<Room, Long> {
    java.util.List<Room> findByLodgingId(Long lodgingId);
}

