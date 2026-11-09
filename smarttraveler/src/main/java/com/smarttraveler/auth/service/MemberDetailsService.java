package com.smarttraveler.auth.service;

import com.smarttraveler.auth.repository.MemberRepository;
import com.smarttraveler.domain.entity.Member;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class MemberDetailsService implements UserDetailsService {
    private final MemberRepository members;
    private final SuspensionReleaseService releases;

    public MemberDetailsService(MemberRepository members, SuspensionReleaseService releases) {
        this.members = members; this.releases = releases;
    }

    @Override
    public UserDetails loadUserByUsername(String loginId) {
        Member member = members.findByLoginId(loginId)
                .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
        if ("SUSPENDED".equals(member.getStatus())) releases.releaseIfExpired(member.getMemberId());
        // The release runs in its own transaction; reload the account for the login decision.
        Member current = members.findById(member.getMemberId()).orElse(member);
        if (current.getPassword() == null || current.getRoleCode() == null) {
            throw new UsernameNotFoundException("Invalid credentials");
        }
        return User.withUsername(current.getLoginId())
                .password(current.getPassword())
                .authorities("ROLE_" + current.getRoleCode())
                .disabled(!"ACTIVE".equals(current.getStatus()) || current.getDeletedAt() != null)
                .build();
    }
}
