package com.smarttraveler.chat;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.sql.Statement;
import java.util.*;

/** 비회원 대화는 브라우저의 서버 세션에 연결하여 다른 사용자의 접근을 막는다. */
@RestController
@RequestMapping("/api/chat")
public class ChatController {
    private final JdbcTemplate jdbc;
    private final ConditionDialogueService dialogue;
    private static final String GUEST = ChatController.class.getName() + ".guest";
    public ChatController(JdbcTemplate jdbc, ConditionDialogueService dialogue) { this.jdbc = jdbc; this.dialogue = dialogue; }

    record MessageInput(@NotBlank @Size(max = 4000) String content) {}
    record ConditionsInput(@NotNull @Size(max = 50000) String conditionsJson, boolean confirmed) {}
    record Message(long id, String role, String content) {}
    record Conditions(String conditionsJson, boolean confirmed) {}
    record Chat(long id, List<Message> messages, Conditions conditions) {}
    record Token(String headerName, String token) {}

    @GetMapping("/csrf")
    Token csrf(CsrfToken token) { return new Token(token.getHeaderName(), token.getToken()); }

    private String guest(HttpSession session) {
        synchronized (session) {
            String token = (String) session.getAttribute(GUEST);
            if (token == null) {
                token = UUID.randomUUID().toString() + UUID.randomUUID().toString();
                session.setAttribute(GUEST, token);
            }
            return token;
        }
    }
    private void checkOwner(long id, HttpSession session) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM chat_sessions WHERE session_id = ? AND guest_token = ?", Integer.class, id, guest(session));
        if (count == null || count != 1) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "대화를 찾을 수 없습니다.");
    }
    // 같은 세션의 수동 저장과 메시지 처리가 서로 덮어쓰지 않도록 순서를 정한다.
    private void lockOwner(long id, HttpSession session) {
        var ids = jdbc.query("SELECT session_id FROM chat_sessions WHERE session_id = ? AND guest_token = ? FOR UPDATE", (rs, row) -> rs.getLong(1), id, guest(session));
        if (ids.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "대화를 찾을 수 없습니다.");
    }
    private long insertMessage(long id, String role, String text) {
        var key = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            var statement = connection.prepareStatement("INSERT INTO chat_messages (session_id, sender_role, content) VALUES (?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, id); statement.setString(2, role); statement.setString(3, text.trim()); return statement;
        }, key);
        jdbc.update("UPDATE chat_sessions SET updated_at = CURRENT_TIMESTAMP WHERE session_id = ?", id);
        return Objects.requireNonNull(key.getKey()).longValue();
    }
    @PostMapping("/sessions") @Transactional
    Chat create(@Valid @RequestBody MessageInput input, HttpSession session) {
        String owner = guest(session);
        var key = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            var statement = connection.prepareStatement("INSERT INTO chat_sessions (guest_token, session_title) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, owner);
            String title = input.content().trim(); statement.setString(2, title.substring(0, Math.min(title.length(), 100)));
            return statement;
        }, key);
        long id = Objects.requireNonNull(key.getKey()).longValue();
        insertMessage(id, "USER", input.content());
        insertMessage(id, "ASSISTANT", dialogue.process(id, input.content()));
        return load(id, session);
    }
    @GetMapping("/sessions/{id}") @Transactional(readOnly = true)
    Chat load(@PathVariable long id, HttpSession session) {
        checkOwner(id, session);
        var messages = jdbc.query("SELECT message_id, sender_role, content FROM chat_messages WHERE session_id = ? ORDER BY created_at, message_id", (rs, row) -> new Message(rs.getLong(1), rs.getString(2), rs.getString(3)), id);
        var conditions = jdbc.query("SELECT conditions_json, confirmed FROM chat_session_conditions WHERE session_id = ?", (rs, row) -> new Conditions(rs.getString(1), rs.getBoolean(2)), id);
        return new Chat(id, messages, conditions.isEmpty() ? null : conditions.get(0));
    }
    @PostMapping("/sessions/{id}/messages") @Transactional
    Chat send(@PathVariable long id, @Valid @RequestBody MessageInput input, HttpSession session) {
        lockOwner(id, session);
        insertMessage(id, "USER", input.content());
        insertMessage(id, "ASSISTANT", dialogue.process(id, input.content()));
        return load(id, session);
    }
    @PutMapping("/sessions/{id}/conditions") @Transactional
    void saveConditions(@PathVariable long id, @Valid @RequestBody ConditionsInput input, HttpSession session) {
        lockOwner(id, session);
        // 작성 도중의 빈 값도 보존하며, MariaDB JSON 컬럼이 형식을 검증한다.
        jdbc.update("INSERT INTO chat_session_conditions (session_id, conditions_json, confirmed) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE conditions_json = VALUES(conditions_json), confirmed = VALUES(confirmed)", id, input.conditionsJson(), input.confirmed());
        jdbc.update("UPDATE chat_sessions SET updated_at = CURRENT_TIMESTAMP WHERE session_id = ?", id);
    }
}
