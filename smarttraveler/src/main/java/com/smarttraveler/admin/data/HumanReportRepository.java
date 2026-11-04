package com.smarttraveler.admin.data;

import org.springframework.data.jpa.repository.JpaRepository;

public interface HumanReportRepository extends JpaRepository<HumanReport, Long> {
    java.util.List<HumanReport> findByCourseId(Long courseId);
}

