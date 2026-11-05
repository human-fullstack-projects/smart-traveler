package com.smarttraveler.auth.service;

import com.smarttraveler.auth.repository.MemberRepository;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemberDetailsService implements UserDetailsService {
    private final MemberRepository members;

    public MemberDetailsService(MemberRepository members) { this.members = members; }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String loginId) {
        var member = members.findByLoginId(loginId)
                .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
        if (member.getPassword() == null || member.getRoleCode() == null) {
            throw new UsernameNotFoundException("Invalid credentials");
        }
        return User.withUsername(member.getLoginId())
                .password(member.getPassword())
                .authorities("ROLE_" + member.getRoleCode())
                .disabled(!"ACTIVE".equals(member.getStatus()) || member.getDeletedAt() != null)
                .build();
    }
}
