package com.smarttraveler.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final MemberRepository members;
    public AuthController(MemberRepository members) { this.members = members; }

    @GetMapping("/csrf")
    public CsrfView csrf(CsrfToken token) {
        return new CsrfView(token.getHeaderName(), token.getToken());
    }

    @GetMapping("/me")
    public MemberView me(Authentication authentication, HttpServletRequest request) {
        var member = members.findByLoginId(authentication.getName()).orElse(null);
        if (member == null || !"ACTIVE".equals(member.getStatus()) || member.getDeletedAt() != null) {
            var session = request.getSession(false);
            if (session != null) session.invalidate();
            SecurityContextHolder.clearContext();
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        // Return only public account fields; never serialize the Member entity/password.
        return new MemberView(member.getMemberId(), member.getLoginId(), member.getNickname(), member.getRoleCode());
    }

    public record CsrfView(String headerName, String token) {}
    public record MemberView(Long memberId, String loginId, String nickname, String roleCode) {}
}
