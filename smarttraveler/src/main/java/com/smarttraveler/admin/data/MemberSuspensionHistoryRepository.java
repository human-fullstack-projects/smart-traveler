package com.smarttraveler.admin.data;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberSuspensionHistoryRepository extends JpaRepository<MemberSuspensionHistory, Long> {
    java.util.List<MemberSuspensionHistory> findByMemberIdOrderByCreatedAtDesc(Long memberId);
}

