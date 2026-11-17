package com.smarttraveler.stay.repository;

import com.smarttraveler.domain.entity.Lodging;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface StayRepository extends JpaRepository<Lodging, Long> {
    List<Lodging> findByStatus(String status);

    @Query(value = """
        SELECT l.* FROM lodging l
        WHERE l.status='ACTIVE' AND EXISTS (
            SELECT 1 FROM room r JOIN room_inventory i ON i.room_id=r.room_id
            WHERE r.lodging_id=l.lodging_id AND r.is_active=TRUE AND i.is_on_sale=TRUE
              AND i.stay_date >= DATE(DATE_ADD(UTC_TIMESTAMP(), INTERVAL 9 HOUR))
        )
        ORDER BY l.lodging_id
        """, nativeQuery = true)
    List<Lodging> findWithInventory();
}
