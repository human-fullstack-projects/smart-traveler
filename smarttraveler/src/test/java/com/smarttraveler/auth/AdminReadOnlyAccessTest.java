package com.smarttraveler.auth;

import com.smarttraveler.auth.config.AuthSecurityConfig;
import com.smarttraveler.auth.repository.MemberRepository;
import com.smarttraveler.auth.repository.SuspensionRepository;
import com.smarttraveler.auth.service.MemberDetailsService;
import com.smarttraveler.auth.service.SuspensionReleaseService;
import com.smarttraveler.chat.ChatSecurity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import tools.jackson.databind.ObjectMapper;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

// 관리자 계정은 여행자 화면에서 예약·결제·휴먼 코스 쓰기·코스 저장·취향 저장을 못 한다 (보안 체인만 검사).
@SpringJUnitConfig(AdminReadOnlyAccessTest.Config.class)
@WebAppConfiguration
class AdminReadOnlyAccessTest {
    @Configuration @EnableWebMvc
    @Import({AuthSecurityConfig.class, ChatSecurity.class, MemberDetailsService.class, SuspensionReleaseService.class})
    static class Config {
        @Bean MemberRepository members() { return mock(MemberRepository.class); }
        @Bean SuspensionRepository suspensions() { return mock(SuspensionRepository.class); }
        @Bean ObjectMapper mapper() { return new ObjectMapper(); }
    }

    @Autowired WebApplicationContext context;
    private MockMvc mvc;

    @BeforeEach void setUp() { mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build(); }

    private static List<MockHttpServletRequestBuilder> blocked() {
        return List.of(
            post("/api/reservations"),
            post("/api/reservations/1/cancel"),
            post("/api/payments/reservations/1/prepare"),
            post("/api/payments/confirm"),
            post("/api/payments/reservations/1/sync"),
            post("/api/human-courses/reservations/1"),
            post("/api/human-courses/1/like"),
            post("/api/human-courses/1/comments"),
            post("/api/human-courses/1/reports"),
            put("/api/human-courses/1"),
            delete("/api/human-courses/1"),
            delete("/api/human-courses/1/comments/2"),
            post("/api/chat/sessions/1/recommendations/2/save"),
            post("/api/chat/guest-course/claim"),
            post("/api/mypage/courses/human/1"),
            post("/api/mypage/courses/import"),
            put("/api/mypage/preferences"));
    }

    private int status(RequestBuilder request) throws Exception { return mvc.perform(request).andReturn().getResponse().getStatus(); }

    @Test void adminIsRefusedWithReadOnlyMessage() throws Exception {
        for (MockHttpServletRequestBuilder request : blocked()) {
            var response = mvc.perform(request.with(user("admin").roles("ADMIN")).with(csrf())).andReturn().getResponse();
            assertThat(response.getStatus()).as(request.toString()).isEqualTo(403);
            assertThat(response.getContentAsString(java.nio.charset.StandardCharsets.UTF_8)).contains("관리자는 권한이 없어요.");
        }
    }

    @Test void travelerAndOwnerPassSecurity() throws Exception {
        for (String role : List.of("USER", "OWNER")) {
            for (MockHttpServletRequestBuilder request : blocked()) {
                // 컨트롤러가 없는 시험 환경이라 보안을 통과하면 404가 된다.
                assertThat(status(request.with(user("u").roles(role)).with(csrf()))).as(role + " " + request).isNotIn(401, 403);
            }
        }
    }

    @Test void adminStillReadsAndUsesAdminAndChat() throws Exception {
        assertThat(status(get("/api/human-courses").with(user("admin").roles("ADMIN")))).isNotIn(401, 403);
        assertThat(status(post("/api/chat/sessions/1/messages").with(user("admin").roles("ADMIN")).with(csrf()))).isNotIn(401, 403);
        assertThat(status(put("/api/mypage/profile").with(user("admin").roles("ADMIN")).with(csrf()))).isNotIn(401, 403);
        assertThat(status(get("/api/admin/places").with(user("admin").roles("ADMIN")))).isNotIn(401, 403);
    }

    @Test void adminWithoutCsrfKeepsGenericMessage() throws Exception {
        var response = mvc.perform(post("/api/reservations").with(user("admin").roles("ADMIN"))).andReturn().getResponse();
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString(java.nio.charset.StandardCharsets.UTF_8)).contains("새로고침");
    }
}
