package com.smarttraveler.auth.repository;

import com.smarttraveler.domain.entity.OwnerProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SignupOwnerRepository extends JpaRepository<OwnerProfile, Long> {
    java.util.Optional<OwnerProfile> findByMemberId(Long memberId);
    boolean existsByBusinessNumber(String businessNumber);
    boolean existsByBusinessNumberAndOwnerIdNot(String businessNumber, Long ownerId);
}
