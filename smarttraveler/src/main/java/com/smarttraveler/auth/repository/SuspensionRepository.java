package com.smarttraveler.auth.repository;

import com.smarttraveler.domain.entity.MemberSuspensionHistory;
import java.util.List;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SuspensionRepository extends JpaRepository<MemberSuspensionHistory, Long> {
    List<MemberSuspensionHistory> findByMemberIdOrderByCreatedAtDesc(Long memberId);
    List<MemberSuspensionHistory> findByReleasedAtIsNullAndSuspendedUntilLessThanEqual(LocalDateTime now);
}
