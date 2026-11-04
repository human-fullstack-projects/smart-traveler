package com.smarttraveler.auth;

import com.smarttraveler.domain.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByLoginId(String loginId);

    // 같은 아이디가 이미 등록되어 있는지 확인
    boolean existsByLoginId(String loginId);
}
