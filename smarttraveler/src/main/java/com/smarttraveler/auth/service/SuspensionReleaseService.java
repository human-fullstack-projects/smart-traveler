package com.smarttraveler.auth.service;

import com.smarttraveler.auth.repository.MemberRepository;
import com.smarttraveler.auth.repository.SuspensionRepository;
import com.smarttraveler.domain.entity.Member;
import com.smarttraveler.domain.entity.MemberSuspensionHistory;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SuspensionReleaseService {
    private final MemberRepository members;
    private final SuspensionRepository suspensions;

    @Transactional
    public void releaseIfExpired(Long memberId) {
        Member member = members.findById(memberId).orElse(null);
        if (member == null || !"SUSPENDED".equals(member.getStatus()) || member.getDeletedAt() != null) return;
        MemberSuspensionHistory active = suspensions.findByMemberIdOrderByCreatedAtDesc(memberId).stream()
            .filter(h -> h.getReleasedAt() == null).findFirst().orElse(null);
        LocalDateTime now = LocalDateTime.now();
        if (active == null || active.getSuspendedUntil() == null || active.getSuspendedUntil().isAfter(now)) return;
        active.setReleasedAt(now);
        suspensions.save(active);
        member.setStatus("ACTIVE");
        member.setUpdatedAt(now);
        members.save(member);
    }

}
