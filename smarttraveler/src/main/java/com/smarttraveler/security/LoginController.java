package com.smarttraveler.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 임시 로그인 (로컬 확인용). 로그인·회원가입은 다른 팀원 담당(WBS 「회원」)이라 합칠 때 그 화면으로 바꾼다.
 * 아이디·비밀번호 입력 없이 버튼 한 번으로 가짜 관리자 계정(m51_admin)으로 들어간다. 권한 검사(ADM-15)는 그대로.
 */
@Controller
public class LoginController {
    static final String DEMO_ADMIN = "m51_admin";

    private final UserDetailsService users;
    private final SecurityContextRepository contexts = new HttpSessionSecurityContextRepository();

    public LoginController(UserDetailsService users) {
        this.users = users;
    }

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    @PostMapping("/login/quick")
    public String quickLogin(HttpServletRequest request, HttpServletResponse response) {
        UserDetails admin = users.loadUserByUsername(DEMO_ADMIN);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(admin, null, admin.getAuthorities()));
        SecurityContextHolder.setContext(context);
        contexts.saveContext(context, request, response);
        return "redirect:/admin";
    }

    @RequestMapping("/403")
    public String forbidden(HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        return "auth/403";
    }
}
