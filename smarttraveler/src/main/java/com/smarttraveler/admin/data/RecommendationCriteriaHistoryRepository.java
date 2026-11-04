package com.smarttraveler.admin.data;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RecommendationCriteriaHistoryRepository extends JpaRepository<RecommendationCriteriaHistory, Long> {
    List<RecommendationCriteriaHistory> findByCriteriaIdOrderByCreatedAtDesc(Long criteriaId);
}
