package com.smarttraveler.auth.config;

import com.smarttraveler.auth.entity.MemberSocialAccount;
import com.smarttraveler.auth.repository.MemberRepository;
import com.smarttraveler.auth.repository.MemberSocialAccountRepository;
import com.smarttraveler.auth.repository.SuspensionRepository;
import com.smarttraveler.auth.service.SocialSessionLogin;
import com.smarttraveler.auth.service.SocialSignupService.PendingSocialProfile;
import com.smarttraveler.auth.service.SuspensionReleaseService;
import com.smarttraveler.domain.entity.Member;
import com.smarttraveler.domain.entity.MemberSuspensionHistory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class SocialOAuthSuccessHandler implements AuthenticationSuccessHandler {
    public static final String PENDING = "PENDING_SOCIAL_PROFILE";
    private final MemberSocialAccountRepository links;
    private final MemberRepository members;
    private final SuspensionRepository suspensions;
    private final SuspensionReleaseService releases;
    private final SocialSessionLogin sessions;
    private final String frontendUrl;

    public SocialOAuthSuccessHandler(MemberSocialAccountRepository links, MemberRepository members,
            SuspensionRepository suspensions, SuspensionReleaseService releases, SocialSessionLogin sessions,
            @Value("${app.frontend-url:http://localhost:5173}") String frontendUrl) {
        this.links = links; this.members = members; this.suspensions = suspensions;
        this.releases = releases; this.sessions = sessions;
        this.frontendUrl = frontendUrl.replaceAll("/+$", "");
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException {
        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
        PendingSocialProfile profile = profile(token);
        Optional<MemberSocialAccount> linked = links.findByProviderAndProviderUserId(
                profile.provider(), profile.providerUserId());
        if (linked.isEmpty()) {
            request.getSession().setAttribute(PENDING, profile);
            sessions.clear(request, response);
            response.sendRedirect(frontendUrl + "/#/signup/social");
            return;
        }
        request.getSession().removeAttribute(PENDING);
        Member member = members.findById(linked.get().getMemberId()).orElse(null);
        if (member != null && "SUSPENDED".equals(member.getStatus())) {
            releases.releaseIfExpired(member.getMemberId());
            member = members.findById(member.getMemberId()).orElse(member);
        }
        String error = blockedMessage(member);
        if (error != null) {
            sessions.clear(request, response);
            response.sendRedirect(frontendUrl + "/?socialError="
                    + URLEncoder.encode(error, StandardCharsets.UTF_8) + "#/login");
            return;
        }
        sessions.login(member, request, response);
        response.sendRedirect(frontendUrl + "/#/");
    }

    public String failureUrl() {
        return frontendUrl + "/?socialError=" + URLEncoder.encode(
                "소셜 로그인을 완료하지 못했어요. 다시 시도해주세요.", StandardCharsets.UTF_8) + "#/login";
    }

    private String blockedMessage(Member member) {
        if (member == null) return "연결된 회원을 찾지 못했어요. 관리자에게 문의해주세요.";
        if ("SUSPENDED".equals(member.getStatus())) {
            Optional<MemberSuspensionHistory> active = suspensions
                    .findByMemberIdOrderByCreatedAtDesc(member.getMemberId()).stream()
                    .filter(history -> history.getReleasedAt() == null).findFirst();
            String reason = active.map(history -> history.getReasonDetail() == null || history.getReasonDetail().isBlank()
                    ? history.getReasonCode() : history.getReasonDetail()).orElse("사유 미등록");
            String until = active.filter(history -> history.getSuspendedUntil() != null)
                    .map(history -> history.getSuspendedUntil().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
                    .orElse("해제일 미정");
            return "계정이 정지되었습니다. 사유: " + reason + " · 해제 예정: " + until;
        }
        if ("WITHDRAWN".equals(member.getStatus()) || member.getDeletedAt() != null)
            return "탈퇴한 계정은 로그인할 수 없습니다.";
        if (!"ACTIVE".equals(member.getStatus())) return "로그인할 수 없는 계정입니다.";
        return null;
    }

    private static PendingSocialProfile profile(OAuth2AuthenticationToken token) {
        String provider = token.getAuthorizedClientRegistrationId().toUpperCase();
        OAuth2User user = token.getPrincipal();
        Map<String, Object> attributes = user.getAttributes();
        String id = String.valueOf("GOOGLE".equals(provider) ? attributes.get("sub") : attributes.get("id"));
        if ("null".equals(id) || id.isBlank()) throw new IllegalArgumentException("소셜 계정 ID가 없습니다.");
        String nickname;
        String email;
        if ("GOOGLE".equals(provider)) {
            nickname = string(attributes.get("name"));
            email = string(attributes.get("email"));
        } else {
            Map<?, ?> account = attributes.get("kakao_account") instanceof Map<?, ?> value ? value : Map.of();
            Map<?, ?> properties = attributes.get("properties") instanceof Map<?, ?> value ? value : Map.of();
            Map<?, ?> profile = account.get("profile") instanceof Map<?, ?> value ? value : Map.of();
            nickname = string(profile.get("nickname"));
            if (nickname == null) nickname = string(properties.get("nickname"));
            email = string(account.get("email"));
        }
        if (nickname == null) nickname = provider.equals("GOOGLE") ? "구글 여행자" : "카카오 여행자";
        if (nickname.length() > 50) nickname = nickname.substring(0, 50);
        if (email != null && email.length() > 255) email = null;
        return new PendingSocialProfile(provider, id, nickname, email);
    }

    private static String string(Object value) {
        return value instanceof String text && !text.isBlank() ? text : null;
    }
}
