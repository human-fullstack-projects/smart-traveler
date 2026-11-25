package com.smarttraveler.chat;

import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.http.HttpMethod;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.web.csrf.CsrfException;

@Configuration
public class ChatSecurity {
    // 채팅 API만 비회원 접근을 허용하고 CSRF 검증은 유지한다.
    @Bean @Order(1)
    SecurityFilterChain chatSecurityFilterChain(HttpSecurity http) throws Exception {
        // 관리자 계정은 채팅으로 대화만 하고 추천 코스 저장·임시 코스 가져오기는 못 한다.
        AuthorizationManager<RequestAuthorizationContext> notAdmin = (authentication, context) ->
                new AuthorizationDecision(!isAdmin(authentication.get()));
        return http.securityMatcher("/api/chat/**")
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/chat/sessions/*/recommendations/*/save", "/api/chat/guest-course/claim").access(notAdmin)
                        .anyRequest().permitAll())
                .exceptionHandling(errors -> errors.accessDeniedHandler((request, response, exception) -> {
                    response.setStatus(403);
                    response.setCharacterEncoding("UTF-8");
                    response.setContentType("application/json");
                    response.getWriter().write(isAdmin(SecurityContextHolder.getContext().getAuthentication())
                            && !(exception instanceof CsrfException)
                            ? "{\"message\":\"관리자는 권한이 없어요.\"}"
                            : "{\"message\":\"요청을 확인할 수 없습니다. 새로고침 후 다시 시도해주세요.\"}");
                }))
                .build();
    }

    private static boolean isAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    // 기존 보안 기본값과 같이 나머지 요청은 인증이 필요하다.
    //@Bean @Order(2)
    SecurityFilterChain otherSecurityFilterChain(HttpSecurity http) throws Exception {
        return http.authorizeHttpRequests(auth -> auth.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll().anyRequest().authenticated())
                .formLogin(Customizer.withDefaults()).httpBasic(Customizer.withDefaults()).build();
    }

}
