package com.smarttraveler.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.smarttraveler.domain.entity.MemberSuspensionHistory;

public interface MemberSuspensionHistoryRepository extends JpaRepository<MemberSuspensionHistory, Long> {
    java.util.List<MemberSuspensionHistory> findByMemberIdOrderByCreatedAtDesc(Long memberId);
}

