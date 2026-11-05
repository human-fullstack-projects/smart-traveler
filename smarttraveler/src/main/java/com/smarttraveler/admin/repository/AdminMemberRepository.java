package com.smarttraveler.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.smarttraveler.domain.entity.Member;
import java.util.Optional;

public interface AdminMemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByLoginId(String loginId);
}
