package com.smarttraveler.admin.data;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OwnerReviewHistoryRepository extends JpaRepository<OwnerReviewHistory, Long> {
    java.util.List<OwnerReviewHistory> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);
}

