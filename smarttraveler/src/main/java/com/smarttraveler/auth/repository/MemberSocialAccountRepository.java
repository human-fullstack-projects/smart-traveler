package com.smarttraveler.auth.repository;

import com.smarttraveler.auth.entity.MemberSocialAccount;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberSocialAccountRepository extends JpaRepository<MemberSocialAccount, Long> {
    Optional<MemberSocialAccount> findByProviderAndProviderUserId(String provider, String providerUserId);
}
