package com.smarttraveler.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import com.smarttraveler.domain.entity.HumanCourseReport;

public interface HumanReportRepository extends JpaRepository<HumanCourseReport, Long> {
    @Query("select r from HumanCourseReport r left join fetch r.reporter")
    java.util.List<HumanCourseReport> findAllWithReporter();
    java.util.List<HumanCourseReport> findByPostId(Long courseId);
}

