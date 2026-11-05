package com.smarttraveler.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.smarttraveler.domain.entity.Course;

public interface CourseRepository extends JpaRepository<Course, Long> {
}

