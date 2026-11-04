package com.smarttraveler.admin.data;

import org.springframework.data.jpa.repository.JpaRepository;

public interface HumanActionHistoryRepository extends JpaRepository<HumanActionHistory, Long> {
    java.util.List<HumanActionHistory> findByCourseIdOrderByCreatedAtDesc(Long courseId);
}

