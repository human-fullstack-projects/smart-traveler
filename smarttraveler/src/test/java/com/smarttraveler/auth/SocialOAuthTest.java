package com.smarttraveler.auth;

import com.smarttraveler.auth.config.SocialOAuthConfig;
import com.smarttraveler.auth.config.SocialOAuthConfig.SocialProviders;
import com.smarttraveler.auth.config.SocialOAuthSuccessHandler;
import com.smarttraveler.auth.controller.SocialAuthController;
import com.smarttraveler.auth.dto.TermConsent;
import com.smarttraveler.auth.entity.MemberSocialAccount;
import com.smarttraveler.auth.repository.MemberRepository;
import com.smarttraveler.auth.repository.MemberSocialAccountRepository;
import com.smarttraveler.auth.repository.SuspensionRepository;
import com.smarttraveler.auth.service.SocialSessionLogin;
import com.smarttraveler.auth.service.SocialSignupService;
import com.smarttraveler.auth.service.SocialSignupService.PendingSocialProfile;
import com.smarttraveler.auth.service.SuspensionReleaseService;
import com.smarttraveler.domain.entity.Member;
import com.smarttraveler.domain.entity.MemberSuspensionHistory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SocialOAuthTest {
    @TempDir Path temp;

    private SocialProviders providers(String googleId, String googleSecret, String kakaoId, String kakaoSecret,
            Path workingDirectory) {
        return SocialOAuthConfig.resolveProviders(googleId, googleSecret, kakaoId, kakaoSecret,
                workingDirectory, temp.resolve("empty-user-profile"));
    }

    @Test void environmentPairWinsOverRepositoryFiles() throws IOException {
        Path working = Files.createDirectory(temp.resolve("work"));
        Path secrets = Files.createDirectory(working.resolve("secrets"));
        Files.writeString(secrets.resolve("google.json"),
                "{\"web\":{\"client_id\":\"file-id\",\"client_secret\":\"file-secret\"}}");
        Files.writeString(secrets.resolve("kakao.json"),
                "{\"rest_api_key\":\"file-id\",\"client_secret\":\"file-secret\"}");

        SocialProviders found = providers("env-google", "env-google-secret", "env-kakao", "env-kakao-secret",
                working);
        assertEquals("env-google", found.googleId());
        assertEquals("env-google-secret", found.googleSecret());
        assertEquals("env-kakao", found.kakaoId());
        assertEquals("env-kakao-secret", found.kakaoSecret());
    }

    @Test void readsJsonFromWorkingDirectorySecrets() throws IOException {
        Path working = Files.createDirectory(temp.resolve("work"));
        Path secrets = Files.createDirectory(working.resolve("secrets"));
        Files.writeString(secrets.resolve("google.json"),
                "{\"web\":{\"client_id\":\"google-id\",\"client_secret\":\"google-secret\"}}");
        Files.writeString(secrets.resolve("kakao.json"),
                "{\"rest_api_key\":\"kakao-id\",\"client_secret\":\"kakao-secret\"}");

        SocialProviders found = providers("", "", "", "", working);
        assertEquals("google-id", found.googleId());
        assertEquals("google-secret", found.googleSecret());
        assertEquals("kakao-id", found.kakaoId());
        assertEquals("kakao-secret", found.kakaoSecret());
    }

    @Test void incompleteEnvironmentPairFallsBackToFile() throws IOException {
        Path working = Files.createDirectory(temp.resolve("work"));
        Path secrets = Files.createDirectory(working.resolve("secrets"));
        Files.writeString(secrets.resolve("google.json"),
                "{\"web\":{\"client_id\":\"file-id\",\"client_secret\":\"file-secret\"}}");

        SocialProviders found = providers("env-id-only", "", "", "", working);
        assertEquals("file-id", found.googleId());
        assertEquals("file-secret", found.googleSecret());
    }

    @Test void readsJsonFromParentSecrets() throws IOException {
        Path working = Files.createDirectory(temp.resolve("smarttraveler"));
        Path secrets = Files.createDirectory(temp.resolve("secrets"));
        Files.writeString(secrets.resolve("google.json"),
                "{\"installed\":{\"client_id\":\"parent-id\",\"client_secret\":\"parent-secret\"}}");

        SocialProviders found = providers("", "", "", "", working);
        assertEquals("parent-id", found.googleId());
        assertEquals("parent-secret", found.googleSecret());
        assertFalse(found.kakao());
    }

    @Test void readsUserProfileOnlyAfterRepositoryFiles() throws IOException {
        Path working = Files.createDirectory(temp.resolve("work"));
        Path profile = Files.createDirectory(temp.resolve("profile"));
        Path userSecrets = Files.createDirectory(profile.resolve(".smart_travel_oauth"));
        Files.writeString(userSecrets.resolve("kakao.json"),
                "{\"rest_api_key\":\"user-id\",\"client_secret\":\"user-secret\"}");

        SocialProviders found = SocialOAuthConfig.resolveProviders("", "", "", "", working, profile);
        assertEquals("user-id", found.kakaoId());
        assertEquals("user-secret", found.kakaoSecret());
    }

    @Test void brokenJsonLeavesProviderUnavailableWithoutThrowing() throws IOException {
        Path working = Files.createDirectory(temp.resolve("work"));
        Path secrets = Files.createDirectory(working.resolve("secrets"));
        Files.writeString(secrets.resolve("google.json"), "{broken");

        SocialProviders found = assertDoesNotThrow(() -> providers("", "", "", "", working));
        assertFalse(found.google());
        assertFalse(found.kakao());
    }

    @Test void providersEndpointReportsBothUnavailableWithoutKeys() {
        SocialAuthController controller = new SocialAuthController(new SocialProviders("", "", "", ""),
                mock(SocialSignupService.class), mock(SocialSessionLogin.class));
        assertFalse(controller.providers().google());
        assertFalse(controller.providers().kakao());
    }

    private OAuth2AuthenticationToken googleUser() {
        DefaultOAuth2User user = new DefaultOAuth2User(List.of(new SimpleGrantedAuthority("ROLE_USER")),
                Map.of("sub", "google-123", "name", "여행자", "email", "traveler@example.test"), "sub");
        return new OAuth2AuthenticationToken(user, user.getAuthorities(), "google");
    }

    private SocialOAuthSuccessHandler handler(MemberSocialAccountRepository links, MemberRepository members,
            SuspensionRepository suspensions, SuspensionReleaseService releases, SocialSessionLogin sessions) {
        return new SocialOAuthSuccessHandler(links, members, suspensions, releases, sessions, "http://localhost:5173");
    }

    @Test void firstLoginLeavesOnlyPendingProfileInSession() throws Exception {
        MemberSocialAccountRepository links = mock(MemberSocialAccountRepository.class);
        MemberRepository members = mock(MemberRepository.class);
        SuspensionRepository suspensions = mock(SuspensionRepository.class);
        SuspensionReleaseService releases = mock(SuspensionReleaseService.class);
        SocialSessionLogin sessions = mock(SocialSessionLogin.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        HttpSession session = mock(HttpSession.class);
        when(request.getSession()).thenReturn(session);
        when(links.findByProviderAndProviderUserId("GOOGLE", "google-123")).thenReturn(Optional.empty());

        handler(links, members, suspensions, releases, sessions)
                .onAuthenticationSuccess(request, response, googleUser());

        verify(session).setAttribute(eq(SocialOAuthSuccessHandler.PENDING),
                eq(new PendingSocialProfile("GOOGLE", "google-123", "여행자", "traveler@example.test")));
        verify(sessions).clear(request, response);
        verify(response).sendRedirect("http://localhost:5173/#/signup/social");
    }

    @Test void linkedLoginUsesOriginalMember() throws Exception {
        MemberSocialAccountRepository links = mock(MemberSocialAccountRepository.class);
        MemberRepository members = mock(MemberRepository.class);
        SuspensionRepository suspensions = mock(SuspensionRepository.class);
        SuspensionReleaseService releases = mock(SuspensionReleaseService.class);
        SocialSessionLogin sessions = mock(SocialSessionLogin.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        HttpSession session = mock(HttpSession.class);
        MemberSocialAccount link = new MemberSocialAccount(); link.setMemberId(21L);
        Member member = new Member(); member.setMemberId(21L); member.setStatus("ACTIVE");
        when(request.getSession()).thenReturn(session);
        when(links.findByProviderAndProviderUserId("GOOGLE", "google-123")).thenReturn(Optional.of(link));
        when(members.findById(21L)).thenReturn(Optional.of(member));

        handler(links, members, suspensions, releases, sessions)
                .onAuthenticationSuccess(request, response, googleUser());

        verify(sessions).login(member, request, response);
        verify(response).sendRedirect("http://localhost:5173/#/");
    }

    @Test void suspendedLinkedMemberIsBlocked() throws Exception {
        MemberSocialAccountRepository links = mock(MemberSocialAccountRepository.class);
        MemberRepository members = mock(MemberRepository.class);
        SuspensionRepository suspensions = mock(SuspensionRepository.class);
        SuspensionReleaseService releases = mock(SuspensionReleaseService.class);
        SocialSessionLogin sessions = mock(SocialSessionLogin.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        HttpSession session = mock(HttpSession.class);
        MemberSocialAccount link = new MemberSocialAccount(); link.setMemberId(21L);
        Member member = new Member(); member.setMemberId(21L); member.setStatus("SUSPENDED");
        MemberSuspensionHistory suspension = new MemberSuspensionHistory();
        suspension.setReasonDetail("이용 규칙 위반"); suspension.setSuspendedUntil(LocalDateTime.of(2026, 11, 20, 18, 0));
        when(request.getSession()).thenReturn(session);
        when(links.findByProviderAndProviderUserId("GOOGLE", "google-123")).thenReturn(Optional.of(link));
        when(members.findById(21L)).thenReturn(Optional.of(member));
        when(suspensions.findByMemberIdOrderByCreatedAtDesc(21L)).thenReturn(List.of(suspension));

        handler(links, members, suspensions, releases, sessions)
                .onAuthenticationSuccess(request, response, googleUser());

        verify(sessions).clear(request, response);
        verify(sessions, never()).login(any(), any(), any());
        verify(response).sendRedirect(contains("socialError="));
    }

    @Test void signupCreatesTravelerLinkAndAllTermConsents() {
        MemberRepository members = mock(MemberRepository.class);
        MemberSocialAccountRepository links = mock(MemberSocialAccountRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(encoder.encode(anyString())).thenReturn("encoded-random-password");
        when(links.findByProviderAndProviderUserId("KAKAO", "456")).thenReturn(Optional.empty());
        when(jdbc.queryForList(anyString())).thenReturn(List.of(
                Map.<String, Object>of("terms_id", 1L, "is_required", true),
                Map.<String, Object>of("terms_id", 2L, "is_required", false)));
        when(members.saveAndFlush(any(Member.class))).thenAnswer(invocation -> {
            Member member = invocation.getArgument(0); member.setMemberId(30L); return member;
        });
        SocialSignupService signup = new SocialSignupService(members, links, encoder, jdbc);

        Member member = signup.create(new PendingSocialProfile("KAKAO", "456", "처음", null), "새 여행자",
                List.of(new TermConsent(1L, true), new TermConsent(2L, false)));

        assertEquals("USER", member.getRoleCode());
        assertEquals("kakao_456", member.getLoginId());
        assertNull(member.getEmail());
        verify(jdbc).update(contains("INSERT INTO member_consents"), eq(30L), eq(1L), eq(true), any());
        verify(jdbc).update(contains("INSERT INTO member_consents"), eq(30L), eq(2L), eq(false), any());
        verify(links).saveAndFlush(any(MemberSocialAccount.class));
    }

}
