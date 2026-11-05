package com.smarttraveler.auth.repository;

import com.smarttraveler.domain.entity.OwnerReviewHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OwnerReviewRepository extends JpaRepository<OwnerReviewHistory, Long> {
    List<OwnerReviewHistory> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);
}
