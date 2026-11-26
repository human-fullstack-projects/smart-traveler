package com.smarttraveler.chat;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 챗봇 대화 메시지를 서버 메모리에 모았다가, 대화 종료(새 대화)·다른 페이지 이동·창 닫기·오래 쉼·서버 정상 종료 때
 * chat_messages에 한 번에 저장한다. 대화 세션(chat_sessions)과 여행 조건(chat_session_conditions)은 지금처럼 바로 저장한다.
 *
 * 아직 저장 전인 메시지는 음수 임시 번호를 쓰고, 저장 뒤에는 임시 번호 → 실제 번호를 기억해 두어
 * 화면이 들고 있는 예전 번호로 와도 같은 메시지를 찾는다. 서버가 갑자기 멈추면 저장 전 메시지는 잃는다.
 */
@Component
public class ChatMessageBuffer {
    private static final Logger log = LoggerFactory.getLogger(ChatMessageBuffer.class);
    /** 이 시간 동안 새 메시지가 없으면 저장한다. */
    static final long IDLE_FLUSH_MILLIS = 30 * 60 * 1000L;
    /** 저장이 끝난 대화의 번호 기억은 이 시간 뒤 지운다. */
    private static final long FORGET_MILLIS = 6 * 60 * 60 * 1000L;
    // JavaScript가 정확히 다루는 범위 안의 음수 (−1조부터 1씩 증가)
    private static final AtomicLong TEMP_IDS = new AtomicLong(-1_000_000_000_000L);

    public record Message(long id, String role, String content, String recommendationJson) {}

    private static final class Pending {
        final long tempId; final String role; final String content; final Instant createdAt;
        String recommendationJson;
        Pending(long tempId, String role, String content) {
            this.tempId = tempId; this.role = role; this.content = content; this.createdAt = Instant.now();
        }
    }
    private static final class Chat {
        final List<Pending> pending = new ArrayList<>();
        final Map<Long, Long> savedIds = new HashMap<>();
        long touched = System.currentTimeMillis();
    }

    private final Map<Long, Chat> chats = new ConcurrentHashMap<>();
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;

    public ChatMessageBuffer(JdbcTemplate jdbc, PlatformTransactionManager manager) {
        this.jdbc = jdbc; this.transactions = new TransactionTemplate(manager);
    }

    static boolean isTemp(long messageId) { return messageId < 0; }

    private Chat chat(long sessionId) { return chats.computeIfAbsent(sessionId, id -> new Chat()); }

    public long add(long sessionId, String role, String content) {
        Chat chat = chat(sessionId);
        synchronized (chat) {
            var message = new Pending(TEMP_IDS.incrementAndGet(), role, content.trim());
            chat.pending.add(message);
            chat.touched = System.currentTimeMillis();
            return message.tempId;
        }
    }

    /** 실패한 요청에서 추가한 메시지를 되돌린다(서버 트랜잭션 롤백과 같은 효과). */
    public void rollbackTo(long sessionId, int pendingCount) {
        Chat chat = chats.get(sessionId);
        if (chat == null) return;
        synchronized (chat) {
            while (chat.pending.size() > pendingCount) chat.pending.removeLast();
        }
    }

    public int pendingCount(long sessionId) {
        Chat chat = chats.get(sessionId);
        if (chat == null) return 0;
        synchronized (chat) { return chat.pending.size(); }
    }

    /** DB에 저장된 메시지 뒤에 아직 저장 전인 메시지를 붙여 대화 전체를 돌려준다. */
    public List<Message> messages(long sessionId) {
        List<Message> result = new ArrayList<>(jdbc.query(
            "SELECT message_id, sender_role, content, recommendation_json FROM chat_messages WHERE session_id = ? ORDER BY created_at, message_id",
            (rs, row) -> new Message(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4)), sessionId));
        Chat chat = chats.get(sessionId);
        if (chat != null) synchronized (chat) {
            for (Pending p : chat.pending) result.add(new Message(p.tempId, p.role, p.content, p.recommendationJson));
        }
        return result;
    }

    /** 임시 번호가 이미 저장됐으면 실제 번호로 바꾼다. 모르는 임시 번호면 그대로 돌려준다. */
    public long resolve(long sessionId, long messageId) {
        if (!isTemp(messageId)) return messageId;
        Chat chat = chats.get(sessionId);
        if (chat == null) return messageId;
        synchronized (chat) { return chat.savedIds.getOrDefault(messageId, messageId); }
    }

    /** 저장 전 챗봇 메시지의 추천 정보. 저장 전 메시지가 아니면 empty. */
    public Optional<String> pendingRecommendation(long sessionId, long messageId) {
        Chat chat = chats.get(sessionId);
        if (chat == null) return Optional.empty();
        synchronized (chat) {
            for (Pending p : chat.pending)
                if (p.tempId == messageId && "ASSISTANT".equals(p.role)) return Optional.ofNullable(p.recommendationJson);
        }
        return Optional.empty();
    }

    public boolean isPending(long sessionId, long messageId) {
        Chat chat = chats.get(sessionId);
        if (chat == null) return false;
        synchronized (chat) { return chat.pending.stream().anyMatch(p -> p.tempId == messageId); }
    }

    /** 추천 정보를 메시지에 붙인다. 저장 전이면 메모리에, 저장됐으면 DB에. */
    public void setRecommendation(long sessionId, long messageId, String json) {
        Chat chat = chats.get(sessionId);
        if (chat != null) synchronized (chat) {
            for (Pending p : chat.pending) if (p.tempId == messageId) { p.recommendationJson = json; return; }
        }
        jdbc.update("UPDATE chat_messages SET recommendation_json=? WHERE message_id=? AND session_id=?", json, resolve(sessionId, messageId), sessionId);
    }

    /**
     * 저장 전 메시지를 chat_messages에 쓴다. 같은 대화의 메시지 처리와 순서가 섞이지 않게 chat_sessions 행을 잠근다.
     * @return 저장한 메시지 수
     */
    public int flush(long sessionId) {
        Chat chat = chats.get(sessionId);
        if (chat == null) return 0;
        Integer saved = transactions.execute(status -> {
            // 진행 중인 메시지 처리(같은 행 FOR UPDATE)가 끝날 때까지 기다린다.
            var exists = jdbc.query("SELECT session_id FROM chat_sessions WHERE session_id = ? FOR UPDATE", (rs, row) -> rs.getLong(1), sessionId);
            List<Pending> batch;
            synchronized (chat) { batch = new ArrayList<>(chat.pending); }
            if (exists.isEmpty()) {
                // 대화 자체가 지워졌으면 남은 메시지도 버린다.
                synchronized (chat) { chat.pending.removeAll(batch); }
                return 0;
            }
            Map<Long, Long> ids = new LinkedHashMap<>();
            for (Pending p : batch) {
                var key = new GeneratedKeyHolder();
                jdbc.update(connection -> {
                    var statement = connection.prepareStatement(
                        "INSERT INTO chat_messages (session_id, sender_role, content, recommendation_json, created_at) VALUES (?, ?, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS);
                    statement.setLong(1, sessionId); statement.setString(2, p.role); statement.setString(3, p.content);
                    statement.setString(4, p.recommendationJson); statement.setTimestamp(5, Timestamp.from(p.createdAt));
                    return statement;
                }, key);
                long realId = Objects.requireNonNull(key.getKey()).longValue();
                ids.put(p.tempId, realId);
                // 「내 코스」에 이미 저장된 추천이면 코스 메타의 메시지 번호도 실제 번호로 바꾼다.
                if (p.recommendationJson != null)
                    jdbc.update("""
                        UPDATE course SET condition_meta = JSON_SET(condition_meta, '$.chat_message_id', ?)
                        WHERE id = JSON_EXTRACT(?, '$.savedCourseId') AND JSON_EXTRACT(condition_meta, '$.chat_message_id') = ?
                        """, realId, p.recommendationJson, p.tempId);
            }
            if (!batch.isEmpty()) jdbc.update("UPDATE chat_sessions SET updated_at = CURRENT_TIMESTAMP WHERE session_id = ?", sessionId);
            synchronized (chat) {
                // 저장하는 사이 추천 정보가 붙었으면(드묾) 실제 행에도 반영한다.
                for (Pending p : batch)
                    if (p.recommendationJson != null)
                        jdbc.update("UPDATE chat_messages SET recommendation_json=? WHERE message_id=? AND recommendation_json IS NULL", p.recommendationJson, ids.get(p.tempId));
                chat.pending.removeAll(batch);
                chat.savedIds.putAll(ids);
                chat.touched = System.currentTimeMillis();
            }
            return batch.size();
        });
        return saved == null ? 0 : saved;
    }

    /** 오래 쉰 대화 저장 + 저장 끝난 대화의 번호 기억 정리. */
    @Scheduled(fixedDelay = 60_000, initialDelay = 60_000)
    public void flushIdle() {
        long now = System.currentTimeMillis();
        for (var entry : chats.entrySet()) {
            Chat chat = entry.getValue();
            boolean idle, forget;
            synchronized (chat) {
                idle = !chat.pending.isEmpty() && now - chat.touched > IDLE_FLUSH_MILLIS;
                forget = chat.pending.isEmpty() && now - chat.touched > FORGET_MILLIS;
            }
            try {
                if (idle) flush(entry.getKey());
                if (forget) chats.remove(entry.getKey(), chat);
            } catch (RuntimeException error) {
                log.warn("챗봇 대화 {} 자동 저장 실패", entry.getKey(), error);
            }
        }
    }

    /** 서버를 정상적으로 끌 때 남은 대화를 모두 저장한다. */
    @PreDestroy
    public void flushAll() {
        for (Long sessionId : List.copyOf(chats.keySet())) {
            try { flush(sessionId); } catch (RuntimeException error) { log.warn("챗봇 대화 {} 종료 저장 실패", sessionId, error); }
        }
    }
}
