package com.smarttraveler.admin.repository;

import com.smarttraveler.domain.entity.RecommendationCriteria;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecommendationCriteriaRepository extends JpaRepository<RecommendationCriteria, Long> {
    boolean existsByName(String name);
}

