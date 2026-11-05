package com.smarttraveler.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.smarttraveler.domain.entity.OwnerReviewHistory;

public interface OwnerReviewHistoryRepository extends JpaRepository<OwnerReviewHistory, Long> {
    java.util.List<OwnerReviewHistory> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);
}

