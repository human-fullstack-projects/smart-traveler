package com.smarttraveler.auth.config;

import com.smarttraveler.auth.service.MemberDetailsService;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;

@Configuration
@EnableWebSecurity
public class AuthSecurityConfig {
    private static final Logger adminAccess = LoggerFactory.getLogger("ADMIN_ACCESS");
    @Bean
    PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, MemberDetailsService users,
                                           PasswordEncoder encoder) throws Exception {
        var provider = new DaoAuthenticationProvider(users);
        provider.setPasswordEncoder(encoder);
        http.authenticationProvider(provider);
        // Keep Spring Security's session CSRF protection and session-fixation protection.
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.GET, "/api/auth/csrf").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/signup").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/auth/terms").permitAll()
                .requestMatchers("/admin", "/admin/**", "/api/admin", "/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/", "/login", "/403", "/error", "/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()
                .anyRequest().authenticated());
        http.requestCache(cache -> cache.disable());
        http.exceptionHandling(errors -> errors
                .authenticationEntryPoint((request, response, exception) ->
                        json(response, 401, "{\"message\":\"로그인이 필요합니다.\"}"))
                .accessDeniedHandler((request, response, exception) ->
                        json(response, 403, "{\"message\":\"요청을 확인할 수 없습니다. 새로고침 후 다시 시도해주세요.\"}")));
        http.formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/api/auth/login")
                .usernameParameter("loginId")
                .passwordParameter("password")
                .successHandler((request, response, authentication) -> {
                    request.getSession().setMaxInactiveInterval(30 * 60);
                    json(response, 200, "{\"authenticated\":true}");
                })
                .failureHandler((request, response, exception) ->
                        json(response, 401, "{\"message\":\"아이디 또는 비밀번호를 확인해주세요. 이용이 제한된 계정도 로그인할 수 없습니다.\"}")));
        http.addFilterBefore(new AdminAccessLogFilter(), UsernamePasswordAuthenticationFilter.class);
        http.logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID")
                .logoutSuccessHandler((request, response, authentication) -> response.setStatus(204)));
        return http.build();
    }

    private static void json(HttpServletResponse response, int status, String body) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(body);
    }

    static final class AdminAccessLogFilter extends OncePerRequestFilter {
        @Override protected boolean shouldNotFilter(HttpServletRequest request) {
            String path = request.getRequestURI();
            return !(path.equals("/admin") || path.startsWith("/admin/")
                || path.equals("/api/admin") || path.startsWith("/api/admin/"));
        }
        @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                FilterChain chain) throws ServletException, IOException {
            try { chain.doFilter(request, response); }
            finally {
                var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
                adminAccess.info("method={} path={} status={} loginId={}", request.getMethod(),
                    request.getRequestURI(), response.getStatus(), auth == null ? "anonymous" : auth.getName());
            }
        }
    }
}
