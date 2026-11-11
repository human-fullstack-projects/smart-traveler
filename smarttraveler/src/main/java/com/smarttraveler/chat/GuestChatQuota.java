package com.smarttraveler.chat;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.Map;

/** 새 대화만 1회 제한한다. 같은 대화의 후속 메시지는 제한하지 않는다. */
@Component
public class GuestChatQuota {
    private static final String USED = GuestChatQuota.class.getName() + ".used";
    private final JdbcTemplate jdbc;

    public GuestChatQuota(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public void reserve(String owner, HttpSession session, Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) return;
        synchronized (session) {
            if (Boolean.TRUE.equals(session.getAttribute(USED))) throw new LimitReached();
            Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM chat_sessions WHERE guest_token = ?", Integer.class, owner);
            if (count != null && count > 0) throw new LimitReached();
            // 커밋 전에도 다른 탭의 동시 요청을 막는다. 실패한 생성은 횟수를 소모하지 않는다.
            session.setAttribute(USED, true);
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCompletion(int status) {
                    if (status != STATUS_COMMITTED) {
                        synchronized (session) {
                            try { session.removeAttribute(USED); }
                            catch (IllegalStateException ignored) { /* 이미 종료된 세션 */ }
                        }
                    }
                }
            });
        }
    }

    static class LimitReached extends RuntimeException {}

    @RestControllerAdvice(assignableTypes = ChatController.class)
    public static class LimitHandler {
        @ExceptionHandler(LimitReached.class)
        ResponseEntity<Map<String, String>> limit() {
            return ResponseEntity.status(403).body(Map.of("code", "GUEST_AI_LIMIT",
                    "message", "비회원 AI 대화 생성은 1회만 가능합니다. 회원가입 후 로그인해 주세요."));
        }
    }
}
