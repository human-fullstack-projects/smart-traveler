package com.smarttraveler.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.smarttraveler.domain.entity.CourseTemplate;

public interface CourseTemplateRepository extends JpaRepository<CourseTemplate, Long> {
}

