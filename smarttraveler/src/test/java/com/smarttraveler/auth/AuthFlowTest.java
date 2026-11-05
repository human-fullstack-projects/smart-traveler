package com.smarttraveler.auth;

import com.smarttraveler.domain.entity.Member;
import com.smarttraveler.auth.config.AuthSecurityConfig;
import com.smarttraveler.auth.controller.AuthController;
import com.smarttraveler.auth.repository.MemberRepository;
import com.smarttraveler.auth.repository.SuspensionRepository;
import com.smarttraveler.domain.entity.MemberSuspensionHistory;
import tools.jackson.databind.ObjectMapper;
import com.smarttraveler.auth.service.MemberDetailsService;
import com.smarttraveler.auth.service.SuspensionReleaseService;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Uses real Security filters and BCrypt with a mocked repository. No database writes.
@SpringJUnitConfig(AuthFlowTest.TestConfig.class)
@WebAppConfiguration
class AuthFlowTest {
    @Configuration
    @EnableWebMvc
    @Import({AuthSecurityConfig.class, MemberDetailsService.class, SuspensionReleaseService.class, AuthController.class})
    static class TestConfig {
        @Bean MemberRepository memberRepository() { return mock(MemberRepository.class); }
        @Bean SuspensionRepository suspensionRepository() { return mock(SuspensionRepository.class); }
        @Bean ObjectMapper objectMapper() { return new ObjectMapper(); }
    }
    @Autowired WebApplicationContext context;
    @Autowired MemberRepository repository;
    @Autowired SuspensionRepository suspensions;
    @Autowired PasswordEncoder encoder;
    MockMvc mvc;
    Member member;

    @BeforeEach
    void setup() {
        reset(repository, suspensions);
        member = new Member();
        member.setMemberId(7L);
        member.setLoginId("test-traveler");
        member.setPassword(encoder.encode("Test-password-123!"));
        member.setNickname("여행자");
        member.setStatus("ACTIVE");
        member.setRoleCode("USER");
        when(repository.findByLoginId("test-traveler")).thenReturn(Optional.of(member));
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private MockHttpSession login() throws Exception {
        return (MockHttpSession) mvc.perform(post("/api/auth/login").with(csrf())
                .param("loginId", "test-traveler").param("password", "Test-password-123!"))
                .andExpect(status().isOk()).andReturn().getRequest().getSession(false);
    }

    @Test void anonymousCannotReadMember() throws Exception {
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }
    @Test void loginNeedsCsrf() throws Exception {
        mvc.perform(post("/api/auth/login").param("loginId", "test-traveler")
                .param("password", "Test-password-123!"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(repository);
    }
    @Test void realCsrfTokenLoginRotatesSessionAndReturnsOnlySafeFields() throws Exception {
        var csrfResult = mvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.token").isString()).andReturn();
        var session = (MockHttpSession) csrfResult.getRequest().getSession(false);
        var token = (CsrfToken) csrfResult.getRequest().getAttribute(CsrfToken.class.getName());
        String oldId = session.getId();
        var result = mvc.perform(post("/api/auth/login").session(session)
                .header(token.getHeaderName(), token.getToken())
                .param("loginId", "test-traveler").param("password", "Test-password-123!"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.authenticated").value(true)).andReturn();
        var authenticated = (MockHttpSession) result.getRequest().getSession(false);
        assertNotEquals(oldId, authenticated.getId());
        assertEquals(1800, authenticated.getMaxInactiveInterval());
        mvc.perform(get("/api/auth/me").session(authenticated)).andExpect(status().isOk())
                .andExpect(jsonPath("$.memberId").value(7))
                .andExpect(jsonPath("$.nickname").value("여행자"))
                .andExpect(jsonPath("$.roleCode").value("USER"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }
    @Test void wrongPasswordAndUnknownUserHaveSameFailure() throws Exception {
        String first = mvc.perform(post("/api/auth/login").with(csrf())
                .param("loginId", "test-traveler").param("password", "wrong"))
                .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();
        String second = mvc.perform(post("/api/auth/login").with(csrf())
                .param("loginId", "missing").param("password", "wrong"))
                .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();
        assertEquals(first, second);
    }
    @Test void suspendedMemberCannotLogin() throws Exception {
        member.setStatus("SUSPENDED");
        var suspension = new MemberSuspensionHistory();
        suspension.setReasonDetail("이용 규칙 위반");
        suspension.setSuspendedUntil(LocalDateTime.of(2026, 11, 20, 18, 0));
        when(suspensions.findByMemberIdOrderByCreatedAtDesc(7L)).thenReturn(java.util.List.of(suspension));
        mvc.perform(post("/api/auth/login").with(csrf()).param("loginId", "test-traveler")
                .param("password", "Test-password-123!")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("이용 규칙 위반")))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("2026-11-20 18:00")));
    }
    @Test void withdrawnMemberGetsOwnFailure() throws Exception {
        member.setStatus("WITHDRAWN");
        mvc.perform(post("/api/auth/login").with(csrf()).param("loginId", "test-traveler")
                .param("password", "Test-password-123!")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("탈퇴한 계정은 로그인할 수 없습니다."));
    }
    @Test void deletedMemberCannotLogin() throws Exception {
        member.setDeletedAt(LocalDateTime.now());
        mvc.perform(post("/api/auth/login").with(csrf()).param("loginId", "test-traveler")
                .param("password", "Test-password-123!")).andExpect(status().isUnauthorized());
    }
    @Test void plaintextDatabasePasswordsAreNotAccepted() throws Exception {
        member.setPassword("Test-password-123!");
        mvc.perform(post("/api/auth/login").with(csrf()).param("loginId", "test-traveler")
                .param("password", "Test-password-123!")).andExpect(status().isUnauthorized());
    }
    @Test void logoutRequiresCsrfAndInvalidatesSession() throws Exception {
        var session = login();
        mvc.perform(post("/api/auth/logout").session(session)).andExpect(status().isForbidden());
        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isOk());
        mvc.perform(post("/api/auth/logout").session(session).with(csrf())).andExpect(status().isNoContent());
        assertTrue(session.isInvalid());
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }
    @Test void disabledMemberIsRejectedOnSessionRefresh() throws Exception {
        var session = login();
        member.setStatus("BANNED");
        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isUnauthorized());
        assertTrue(session.isInvalid());
    }
}
