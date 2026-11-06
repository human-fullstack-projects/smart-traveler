package com.smarttraveler.stay.repository;

import com.smarttraveler.domain.entity.Lodging;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StayRepository extends JpaRepository<Lodging, Long> {
    List<Lodging> findByStatus(String status);
}
