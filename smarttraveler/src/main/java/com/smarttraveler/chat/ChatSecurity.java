package com.smarttraveler.chat;

import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class ChatSecurity {
    // 채팅 API만 비회원 접근을 허용하고 CSRF 검증은 유지한다.
    @Bean @Order(1)
    SecurityFilterChain chatSecurityFilterChain(HttpSecurity http) throws Exception {
        return http.securityMatcher("/api/chat/**")
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll()).build();
    }

    // 기존 보안 기본값과 같이 나머지 요청은 인증이 필요하다.
    // @Bean @Order(2)
    // SecurityFilterChain otherSecurityFilterChain(HttpSecurity http) throws Exception {
    //     return http.authorizeHttpRequests(auth -> auth.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll().anyRequest().authenticated())
    //             .formLogin(Customizer.withDefaults()).httpBasic(Customizer.withDefaults()).build();
    // }
}
