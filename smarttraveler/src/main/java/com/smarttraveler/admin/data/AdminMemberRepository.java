package com.smarttraveler.admin.data;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AdminMemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByLoginId(String loginId);
}
