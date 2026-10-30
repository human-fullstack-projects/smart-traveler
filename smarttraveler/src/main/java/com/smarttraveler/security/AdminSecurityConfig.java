package com.smarttraveler.security;

import com.smarttraveler.admin.data.Member;
import com.smarttraveler.admin.data.MemberRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
public class AdminSecurityConfig {
    private static final Logger accessLog = LoggerFactory.getLogger("ADMIN_ACCESS");

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService memberUserDetailsService(MemberRepository members) {
        return loginId -> {
            Member member = members.findByLoginId(loginId)
                    .orElseThrow(() -> new UsernameNotFoundException("계정을 찾을 수 없습니다."));
            String role = member.getRoleCode();
            return User.withUsername(member.getLoginId())
                    .password(member.getPassword())
                    .authorities("ROLE_" + (role == null ? "USER" : role.toUpperCase()))
                    .disabled(!"ACTIVE".equalsIgnoreCase(member.getStatus()))
                    .build();
        };
    }

    @Bean
    SecurityFilterChain adminSecurityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(authorize -> authorize
                    .requestMatchers("/admin", "/admin/**").hasRole("ADMIN")
                    .requestMatchers("/", "/login", "/403", "/css/**", "/js/**", "/images/**", "/error").permitAll()
                    .anyRequest().permitAll())
                .formLogin(form -> form.loginPage("/login")
                    .loginProcessingUrl("/login")
                    .defaultSuccessUrl("/admin", true)
                    .failureUrl("/login?error")
                    .permitAll())
                .logout(logout -> logout.logoutUrl("/logout").logoutSuccessUrl("/login?logout"))
                .exceptionHandling(ex -> ex.accessDeniedPage("/403"))
                .addFilterBefore(new AdminAccessLogFilter(), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    static final class AdminAccessLogFilter extends OncePerRequestFilter {
        @Override
        protected boolean shouldNotFilter(HttpServletRequest request) {
            String path = request.getRequestURI();
            return !(path.equals("/admin") || path.startsWith("/admin/"));
        }

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                FilterChain chain) throws ServletException, IOException {
            try {
                chain.doFilter(request, response);
            } finally {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                String loginId = auth != null && auth.isAuthenticated() ? auth.getName() : "anonymous";
                boolean admin = auth != null && auth.getAuthorities().stream()
                        .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
                String outcome = !admin || response.getStatus() == 403 ? "DENIED" : "ACCESS";
                accessLog.info("{} method={} path={} status={} loginId={}",
                        outcome, request.getMethod(), request.getRequestURI(), response.getStatus(), loginId);
            }
        }
    }
}
