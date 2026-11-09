package com.smarttraveler.auth.controller;

import com.smarttraveler.auth.config.SocialOAuthConfig.SocialProviders;
import com.smarttraveler.auth.config.SocialOAuthSuccessHandler;
import com.smarttraveler.auth.dto.TermConsent;
import com.smarttraveler.auth.service.SocialSessionLogin;
import com.smarttraveler.auth.service.SocialSignupService;
import com.smarttraveler.auth.service.SocialSignupService.PendingSocialProfile;
import com.smarttraveler.domain.entity.Member;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth/social")
public class SocialAuthController {
    private final SocialProviders providers;
    private final SocialSignupService signup;
    private final SocialSessionLogin sessions;
    public SocialAuthController(SocialProviders providers, SocialSignupService signup, SocialSessionLogin sessions) {
        this.providers = providers; this.signup = signup; this.sessions = sessions;
    }

    @GetMapping("/providers")
    public ProviderView providers() { return new ProviderView(providers.google(), providers.kakao()); }

    @GetMapping("/pending")
    public PendingView pending(HttpSession session) {
        PendingSocialProfile profile = (PendingSocialProfile) session.getAttribute(SocialOAuthSuccessHandler.PENDING);
        if (profile == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "소셜 가입 정보가 없습니다.");
        return new PendingView(profile.provider(), profile.nickname(), profile.email());
    }

    @PostMapping("/signup")
    public AuthController.MemberView signup(@RequestBody SignupRequest request,
            HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
        HttpSession session = servletRequest.getSession(false);
        PendingSocialProfile profile = session == null ? null
                : (PendingSocialProfile) session.getAttribute(SocialOAuthSuccessHandler.PENDING);
        if (profile == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "소셜 로그인부터 다시 진행해주세요.");
        Member member = signup.create(profile, request.nickname(), request.consents());
        session.removeAttribute(SocialOAuthSuccessHandler.PENDING);
        sessions.login(member, servletRequest, servletResponse);
        return new AuthController.MemberView(member.getMemberId(), member.getLoginId(),
                member.getNickname(), member.getRoleCode());
    }

    public record ProviderView(boolean google, boolean kakao) {}
    public record PendingView(String provider, String nickname, String email) {}
    public record SignupRequest(String nickname, List<TermConsent> consents) {}
}
