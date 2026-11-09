package com.smarttraveler.auth.service;

import com.smarttraveler.domain.entity.Member;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;

@Service
public class SocialSessionLogin {
    private final MemberDetailsService users;
    private final HttpSessionSecurityContextRepository contexts = new HttpSessionSecurityContextRepository();
    public SocialSessionLogin(MemberDetailsService users) { this.users = users; }

    public void login(Member member, HttpServletRequest request, HttpServletResponse response) {
        UserDetails details = users.loadUserByUsername(member.getLoginId());
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                details, null, details.getAuthorities());
        if (request.getSession(false) != null) request.changeSessionId();
        request.getSession().setMaxInactiveInterval(30 * 60);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        contexts.saveContext(context, request, response);
    }

    public void clear(HttpServletRequest request, HttpServletResponse response) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        SecurityContextHolder.setContext(context);
        contexts.saveContext(context, request, response);
    }
}
