package com.smarttraveler.admin.repository;

import com.smarttraveler.domain.entity.RecommendationCriteriaHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RecommendationCriteriaHistoryRepository extends JpaRepository<RecommendationCriteriaHistory, Long> {
    List<RecommendationCriteriaHistory> findByCriteriaIdOrderByCreatedAtDescIdDesc(Long criteriaId);
}
