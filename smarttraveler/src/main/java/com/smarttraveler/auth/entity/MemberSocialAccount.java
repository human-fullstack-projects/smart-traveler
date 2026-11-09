package com.smarttraveler.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

@Entity
@Table(name = "member_social_account", uniqueConstraints =
        @UniqueConstraint(name = "uk_member_social_provider_user", columnNames = {"provider", "provider_user_id"}))
public class MemberSocialAccount {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "social_id") private Long socialId;
    @Column(name = "member_id", nullable = false) private Long memberId;
    @Column(name = "provider", nullable = false, length = 20) private String provider;
    @Column(name = "provider_user_id", nullable = false, length = 255) private String providerUserId;
    @Column(name = "email", length = 255) private String email;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    public Long getMemberId() { return memberId; }
    public void setMemberId(Long value) { memberId = value; }
    public void setProvider(String value) { provider = value; }
    public void setProviderUserId(String value) { providerUserId = value; }
    public void setEmail(String value) { email = value; }
    public void setCreatedAt(LocalDateTime value) { createdAt = value; }
}
