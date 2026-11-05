package com.smarttraveler.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.smarttraveler.domain.entity.HumanCourseReport;

public interface HumanReportRepository extends JpaRepository<HumanCourseReport, Long> {
    java.util.List<HumanCourseReport> findByPostId(Long courseId);
}

