//<<<<<<< HEAD:smarttraveler/src/main/java/com/smarttraveler/auth/MemberRepository.java
////package com.smarttraveler.auth;
////
////import com.smarttraveler.domain.entity.Member;
////import org.springframework.data.jpa.repository.JpaRepository;
////import java.util.Optional;
////
////public interface MemberRepository extends JpaRepository<Member, Long> {
////    Optional<Member> findByLoginId(String loginId);
////<<<<<<< HEAD
////
////    // 같은 아이디가 이미 등록되어 있는지 확인
////=======
////>>>>>>> origin/main
////    boolean existsByLoginId(String loginId);
////}
//package com.smarttraveler.auth;
//=======
//package com.smarttraveler.auth.repository;
//>>>>>>> origin/main:smarttraveler/src/main/java/com/smarttraveler/auth/repository/MemberRepository.java
//
//import com.smarttraveler.domain.entity.Member;
//import org.springframework.data.jpa.repository.JpaRepository;
//import java.util.Optional;
//
//public interface MemberRepository extends JpaRepository<Member, Long> {
//    Optional<Member> findByLoginId(String loginId);
//
//    boolean existsByLoginId(String loginId);
//}
package com.smarttraveler.auth.repository;

import com.smarttraveler.domain.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByLoginId(String loginId);
    boolean existsByLoginId(String loginId);
}