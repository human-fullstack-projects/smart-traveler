package com.smarttraveler.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.smarttraveler.domain.entity.Lodging;

public interface LodgingRepository extends JpaRepository<Lodging, Long> {
}

