package com.smarttraveler.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import tools.jackson.databind.ObjectMapper;
import com.smarttraveler.auth.config.SocialOAuthSuccessHandler;
import com.smarttraveler.auth.service.SocialSignupService.PendingSocialProfile;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/** 회원가입·소셜 가입 본인인증 이름·번호가 실제 DB에 저장되고, 같은 번호는 두 번 가입되지 않는지 (임시 DB에서만 실행). */
@EnabledIfEnvironmentVariable(named = "IDENTITY_DB_TEST", matches = "true")
@SpringBootTest
class IdentitySignupDatabaseTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper mapper;
    @Autowired WebApplicationContext context;
    MockMvc mvc;
    final List<String> logins = new ArrayList<>();
    final String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    final java.util.Random random = new java.util.Random();
    final String phone = String.format("010-9%03d-%04d", random.nextInt(1000), random.nextInt(10000));

    @BeforeEach void setup() { mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build(); }

    @AfterEach void cleanup() {
        for (String login : logins) {
            jdbc.update("DELETE FROM member_consents WHERE member_id IN (SELECT member_id FROM members WHERE login_id=?)", login);
            jdbc.update("DELETE FROM member_social_account WHERE member_id IN (SELECT member_id FROM members WHERE login_id=?)", login);
            jdbc.update("DELETE FROM members WHERE login_id=?", login);
        }
    }

    List<Map<String, Object>> consents(MockHttpSession browser) throws Exception {
        var response = mvc.perform(get("/api/auth/terms").session(browser)).andExpect(status().isOk()).andReturn();
        List<Map<String, Object>> list = new ArrayList<>();
        for (var term : mapper.readTree(response.getResponse().getContentAsString()))
            list.add(Map.of("termsId", term.path("termsId").asLong(), "agreed", true));
        return list;
    }

    @Test void travelerAndSocialSignupSaveIdentityAndBlockSamePhone() throws Exception {
        String digits = phone.replace("-", "");
        var browser = new MockHttpSession();
        String login = "idn_" + suffix;
        logins.add(login);
        mvc.perform(get("/api/auth/phone-check").param("phone", phone)).andExpect(jsonPath("$.available").value(true));
        String body = mapper.writeValueAsString(Map.of("role", "USER", "loginId", login, "password", "Identity123",
                "nickname", "본인인증 " + suffix, "email", login + "@example.invalid", "consents", consents(browser),
                "identityName", "홍길동", "identityPhone", phone));
        mvc.perform(post("/api/auth/signup").session(browser).with(csrf()).contentType("application/json").content(body))
                .andExpect(status().isCreated());
        Map<String, Object> row = jdbc.queryForMap("SELECT real_name, phone_number, identity_verified_at FROM members WHERE login_id=?", login);
        assertEquals("홍길동", row.get("real_name"));
        assertEquals(digits, row.get("phone_number"));
        assertEquals(true, row.get("identity_verified_at") != null);
        mvc.perform(get("/api/auth/phone-check").param("phone", digits)).andExpect(jsonPath("$.available").value(false));

        // 같은 번호로 소셜 가입 → 409
        var social = new MockHttpSession();
        social.setAttribute(SocialOAuthSuccessHandler.PENDING, new PendingSocialProfile("KAKAO", "idn" + suffix, "카카오", null));
        logins.add("kakao_idn" + suffix);
        String socialBody = mapper.writeValueAsString(Map.of("nickname", "소셜 " + suffix, "consents", consents(social),
                "identityName", "김철수", "identityPhone", phone));
        mvc.perform(post("/api/auth/social/signup").session(social).with(csrf()).contentType("application/json").content(socialBody))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.message").value("이미 가입된 휴대폰 번호예요."));

        // 다른 번호로 소셜 가입 → 저장
        String other = "010-8" + phone.substring(5);
        socialBody = mapper.writeValueAsString(Map.of("nickname", "소셜 " + suffix, "consents", consents(social),
                "identityName", "김철수", "identityPhone", other));
        mvc.perform(post("/api/auth/social/signup").session(social).with(csrf()).contentType("application/json").content(socialBody))
                .andExpect(status().isOk());
        Map<String, Object> socialRow = jdbc.queryForMap("SELECT real_name, phone_number FROM members WHERE login_id=?", "kakao_idn" + suffix);
        assertEquals("김철수", socialRow.get("real_name"));
        assertEquals(other.replace("-", ""), socialRow.get("phone_number"));
    }
}
