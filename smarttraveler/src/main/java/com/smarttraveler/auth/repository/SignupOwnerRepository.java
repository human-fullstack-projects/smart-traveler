package com.smarttraveler.auth.repository;

import com.smarttraveler.domain.entity.OwnerProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SignupOwnerRepository extends JpaRepository<OwnerProfile, Long> {}
