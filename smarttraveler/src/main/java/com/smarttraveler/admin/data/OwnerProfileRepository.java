package com.smarttraveler.admin.data;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OwnerProfileRepository extends JpaRepository<OwnerProfile, Long> {
    java.util.Optional<OwnerProfile> findByMemberId(Long memberId);
}

