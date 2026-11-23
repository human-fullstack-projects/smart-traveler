package com.smarttraveler.chat;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.security.core.Authentication;
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
    private final ChatPersonalization personalization;
    private final CourseRegionController regions;
    private final RecommendedCourseService recommendations;
    private final GuestRecommendedCourseService guestCourses;
    private final ChatMessageBuffer buffer;
    private final GuestChatQuota guestQuota;
    private static final String GUEST = ChatController.class.getName() + ".guest";
    public ChatController(JdbcTemplate jdbc, ConditionDialogueService dialogue, ChatPersonalization personalization, CourseRegionController regions, RecommendedCourseService recommendations, GuestRecommendedCourseService guestCourses, ChatMessageBuffer buffer, GuestChatQuota guestQuota) {
        this.jdbc = jdbc; this.dialogue = dialogue; this.personalization = personalization;
        this.regions = regions;
        this.recommendations = recommendations;
        this.guestCourses = guestCourses;
        this.buffer = buffer;
        this.guestQuota = guestQuota;
    }

    record MessageInput(@NotBlank @Size(max = 4000) String content, @jakarta.validation.constraints.Positive Long regionId) {
        MessageInput(String content) { this(content, null); }
    }
    record ConditionsInput(@NotNull @Size(max = 50000) String conditionsJson, boolean confirmed) {}
    record Message(long id, String role, String content, tools.jackson.databind.JsonNode recommendation) {}
    record Conditions(String conditionsJson, boolean confirmed) {}
    record Chat(long id, List<Message> messages, Conditions conditions) {}
    record Token(String headerName, String token) {}

    @GetMapping("/csrf")
    Token csrf(CsrfToken token) { return new Token(token.getHeaderName(), token.getToken()); }

    private String guest(HttpSession session) {
        synchronized (session) {
            String identity = personalization.ownerKey();
            if (!identity.equals(session.getAttribute(GUEST + ".identity"))) {
                session.removeAttribute(GUEST);
                session.setAttribute(GUEST + ".identity", identity);
            }
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
    // 메시지는 바로 DB에 쓰지 않고 보관함에 모았다가 대화 종료·페이지 이동·창 닫기 때 저장한다(ChatMessageBuffer).
    private long insertMessage(long id, String role, String text) {
        return buffer.add(id, role, text);
    }
    /** 요청이 실패하면 이번 요청에서 보관함에 넣은 메시지도 되돌린다. */
    private void replyOrRollback(long id, MessageInput input, int before, boolean withUserMessage) {
        try {
            if (withUserMessage) insertMessage(id, "USER", input.content());
            reply(id, input);
        } catch (RuntimeException error) {
            buffer.rollbackTo(id, before);
            throw error;
        }
    }
    private void reply(long id, MessageInput input) {
        if (input.regionId() != null) {
            insertMessage(id, "ASSISTANT", dialogue.selectRegion(id, regions.find(input.regionId())));
            return;
        }
        var result = dialogue.processResult(id, input.content(), personalization.savedPreferences());
        long messageId = insertMessage(id, "ASSISTANT", result.message());
        if (result.recommendedCourseId() != null) {
            if (result.recommendConditions() != null) recommendations.record(id, messageId, result.recommendedCourseId(), result.recommendConditions());
            else recommendations.record(id, messageId, result.recommendedCourseId());
        } else if (result.recommendedPlaces() != null) {
            recommendations.recordLodgingItinerary(id, messageId, result.recommendConditions(), result.recommendedPlaces());
        }
    }
    @PostMapping("/sessions") @Transactional
    Chat create(@Valid @RequestBody MessageInput input, HttpSession session, Authentication authentication) {
        String owner = guest(session);
        guestQuota.reserve(owner, session, authentication);
        var key = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            var statement = connection.prepareStatement("INSERT INTO chat_sessions (guest_token, session_title, member_id) VALUES (?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, owner);
            String title = input.content().trim(); statement.setString(2, title.substring(0, Math.min(title.length(), 100)));
            statement.setObject(3, personalization.memberId());
            return statement;
        }, key);
        long id = Objects.requireNonNull(key.getKey()).longValue();
        insertMessage(id, "ASSISTANT", ConditionQuestions.INTRO);
        replyOrRollback(id, input, 0, true);
        return load(id, session);
    }
    @GetMapping("/sessions/{id}") @Transactional(readOnly = true)
    Chat load(@PathVariable long id, HttpSession session) {
        checkOwner(id, session);
        var messages = buffer.messages(id).stream()
            .map(m -> new Message(m.id(), m.role(), m.content(), recommendations.read(m.recommendationJson()))).toList();
        var conditions = jdbc.query("SELECT conditions_json, confirmed FROM chat_session_conditions WHERE session_id = ?", (rs, row) -> new Conditions(rs.getString(1), rs.getBoolean(2)), id);
        return new Chat(id, messages, conditions.isEmpty() ? null : conditions.get(0));
    }
    @PostMapping("/sessions/{id}/messages") @Transactional
    Chat send(@PathVariable long id, @Valid @RequestBody MessageInput input, HttpSession session) {
        lockOwner(id, session);
        replyOrRollback(id, input, buffer.pendingCount(id), true);
        return load(id, session);
    }
    /** 대화 종료(새 대화)·다른 페이지 이동·창 닫기 때 화면이 부른다. 모아 둔 메시지를 DB에 저장한다. */
    @PostMapping("/sessions/{id}/flush")
    Chat flush(@PathVariable long id, HttpSession session) {
        checkOwner(id, session);
        buffer.flush(id);
        return load(id, session);
    }
    record Region(Long sidoId, Long sigunguId, String name) {}
    record SavedCourse(long courseId, Region region) {}
    record SaveInput(String startDate) {}
    @PostMapping("/sessions/{id}/recommendations/{messageId}/guest")
    GuestRecommendedCourseService.Preview storeGuestRecommendation(@PathVariable long id, @PathVariable long messageId, HttpSession session) {
        if (personalization.memberId() != null) throw new ResponseStatusException(HttpStatus.CONFLICT, "회원 코스 저장을 이용해 주세요.");
        checkOwner(id, session);
        return guestCourses.store(session, id, messageId);
    }
    @PostMapping("/sessions/{id}/recommendations/{messageId}/save") @Transactional
    SavedCourse saveRecommendation(@PathVariable long id, @PathVariable long messageId,
                                   @RequestBody(required = false) SaveInput input, HttpSession session) {
        Long memberId = personalization.memberId();
        if (memberId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        lockOwner(id, session);
        var saved = recommendations.save(memberId, id, messageId, input == null ? null : input.startDate());
        return new SavedCourse(saved.courseId(), new Region(saved.sidoId(), saved.sigunguId(), saved.regionName()));
    }
    @PutMapping("/sessions/{id}/conditions") @Transactional
    void saveConditions(@PathVariable long id, @Valid @RequestBody ConditionsInput input, HttpSession session) {
        lockOwner(id, session);
        // 작성 도중의 빈 값도 보존하며, MariaDB JSON 컬럼이 형식을 검증한다.
        jdbc.update("INSERT INTO chat_session_conditions (session_id, conditions_json, confirmed) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE conditions_json = VALUES(conditions_json), confirmed = VALUES(confirmed)", id, input.conditionsJson(), input.confirmed());
        jdbc.update("UPDATE chat_sessions SET updated_at = CURRENT_TIMESTAMP WHERE session_id = ?", id);
    }
}
