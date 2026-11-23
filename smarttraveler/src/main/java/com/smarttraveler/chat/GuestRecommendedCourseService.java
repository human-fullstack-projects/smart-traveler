package com.smarttraveler.chat;

import jakarta.servlet.http.HttpSession;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;

@Service
public class GuestRecommendedCourseService {
    static final String PENDING = GuestRecommendedCourseService.class.getName() + ".pending";
    private static final String SAVED = GuestRecommendedCourseService.class.getName() + ".saved";
    private final JdbcTemplate jdbc;
    private final RecommendedCourseService courses;
    private final TransactionTemplate transactions;
    private final ChatMessageBuffer buffer;

    record Pending(long sessionId, long messageId, String snapshot, Long memberId) implements Serializable {}
    record Saved(long memberId, long courseId) implements Serializable {}
    public record Preview(long messageId, String title, String destination, String startDate, int durationDays) {}

    public GuestRecommendedCourseService(JdbcTemplate jdbc, RecommendedCourseService courses, PlatformTransactionManager manager, ChatMessageBuffer buffer) {
        this.jdbc = jdbc; this.courses = courses; this.transactions = new TransactionTemplate(manager); this.buffer = buffer;
    }

    public Preview store(HttpSession session, long sessionId, long messageId) {
        return store(session, sessionId, messageId, null, null);
    }

    public Preview store(HttpSession session, long sessionId, long messageId, String startDate, Long courseId) {
        synchronized (session) {
            Pending current = (Pending) session.getAttribute(PENDING);
            if (current != null && current.memberId() != null) throw conflict();
            List<String> rows;
            if (buffer.isPending(sessionId, messageId)) {
                // 아직 저장 전(보관함) 메시지
                var guestChat = jdbc.query("SELECT session_id FROM chat_sessions WHERE session_id=? AND member_id IS NULL", (rs, row) -> rs.getLong(1), sessionId);
                rows = guestChat.isEmpty() ? List.of() : buffer.pendingRecommendation(sessionId, messageId).stream().toList();
            } else {
                rows = jdbc.query("""
                    SELECT m.recommendation_json FROM chat_messages m JOIN chat_sessions s ON s.session_id=m.session_id
                    WHERE m.message_id=? AND m.session_id=? AND m.sender_role='ASSISTANT' AND s.member_id IS NULL
                      AND m.recommendation_json IS NOT NULL
                    """, (rs, row) -> rs.getString(1), buffer.resolve(sessionId, messageId), sessionId);
            }
            if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "보관할 추천 코스가 없습니다.");
            if (current == null || newer(messageId, current.messageId())) {
                current = new Pending(sessionId, messageId, courses.guestSnapshot(rows.getFirst(), startDate, courseId), null);
                session.setAttribute(PENDING, current);
            }
            return preview(current);
        }
    }

    public Preview status(HttpSession session, Long memberId) {
        synchronized (session) {
            Pending pending = (Pending) session.getAttribute(PENDING);
            if (pending == null || (pending.memberId() != null && !pending.memberId().equals(memberId))) return null;
            return preview(pending);
        }
    }

    public Long claim(HttpSession session, long memberId) {
        Pending pending;
        synchronized (session) {
            pending = (Pending) session.getAttribute(PENDING);
            if (pending == null) {
                Saved saved = (Saved) session.getAttribute(SAVED);
                return saved != null && saved.memberId() == memberId ? saved.courseId() : null;
            }
            if (pending.memberId() != null && pending.memberId() != memberId) throw conflict();
            pending = new Pending(pending.sessionId(), pending.messageId(), pending.snapshot(), memberId);
            session.setAttribute(PENDING, pending);
        }
        Pending claimed = pending;
        // Keep the pending course if any insert or the final commit fails.
        Long courseId = transactions.execute(transaction -> {
            var owners = jdbc.query("SELECT member_id FROM chat_sessions WHERE session_id=? FOR UPDATE",
                (rs, row) -> rs.getObject(1, Long.class), claimed.sessionId());
            if (owners.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "보관한 대화를 찾을 수 없습니다.");
            if (owners.getFirst() != null && owners.getFirst() != memberId) throw conflict();
            jdbc.update("UPDATE chat_sessions SET member_id=? WHERE session_id=?", memberId, claimed.sessionId());
            JsonNode snapshot = courses.read(claimed.snapshot());
            Long chosenCourseId = snapshot.path("candidateCourseIds").size() > 1
                ? snapshot.path("sourceCourseId").asLong() : null;
            return courses.save(memberId, claimed.sessionId(), claimed.messageId(),
                snapshot.path("conditions").path("startDate").asText(null), chosenCourseId).courseId();
        });
        Objects.requireNonNull(courseId);
        synchronized (session) {
            // The transaction has committed before clearing the session data.
            try {
                Pending current = (Pending) session.getAttribute(PENDING);
                if (current != null && current.messageId() == claimed.messageId() && Objects.equals(current.memberId(), memberId))
                    session.removeAttribute(PENDING);
                session.setAttribute(SAVED, new Saved(memberId, courseId));
            } catch (IllegalStateException ignored) { /* A concurrent logout already expired the session. */ }
        }
        return courseId;
    }

    private Preview preview(Pending pending) {
        JsonNode snapshot = courses.read(pending.snapshot());
        return new Preview(pending.messageId(), snapshot.path("title").asText(), snapshot.path("destination").asText(),
            snapshot.path("conditions").path("startDate").asText(), snapshot.path("conditions").path("durationDays").asInt());
    }
    /** 보관함(음수 임시 번호) 메시지는 DB에 저장된 메시지보다 항상 나중 메시지다. */
    private static boolean newer(long candidate, long current) {
        if (ChatMessageBuffer.isTemp(candidate) != ChatMessageBuffer.isTemp(current)) return ChatMessageBuffer.isTemp(candidate);
        return current < candidate;
    }
    private ResponseStatusException conflict() {
        return new ResponseStatusException(HttpStatus.CONFLICT, "보관한 코스는 처음 이전을 시도한 계정으로만 저장할 수 있습니다.");
    }
}
