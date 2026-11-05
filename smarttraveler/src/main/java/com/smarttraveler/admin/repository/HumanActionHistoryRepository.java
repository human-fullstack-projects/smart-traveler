package com.smarttraveler.admin.repository;

import com.smarttraveler.domain.entity.HumanActionHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HumanActionHistoryRepository extends JpaRepository<HumanActionHistory, Long> {
    java.util.List<HumanActionHistory> findByCourseIdOrderByCreatedAtDesc(Long courseId);
}

