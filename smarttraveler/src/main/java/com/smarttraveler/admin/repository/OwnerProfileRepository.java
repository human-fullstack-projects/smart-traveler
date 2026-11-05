package com.smarttraveler.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.smarttraveler.domain.entity.OwnerProfile;

public interface OwnerProfileRepository extends JpaRepository<OwnerProfile, Long> {
    java.util.Optional<OwnerProfile> findByMemberId(Long memberId);
}

