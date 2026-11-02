package com.smarttraveler.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@DynamicInsert
@DynamicUpdate
@Entity
@Table(name = "chat_messages")
/** 대화 메시지 */
public class ChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id", nullable = false)
    /** 메시지 번호 */
    private Long messageId;

    @Column(name = "session_id", nullable = false)
    /** 대화 세션 번호 */
    private Long sessionId;

    @Column(name = "sender_role", nullable = false, length = 20)
    /** 발신자 구분 */
    private String senderRole;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    /** 메시지 내용 */
    private String content;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at", nullable = false)
    /** 생성일시 */
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", referencedColumnName = "session_id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "FK_chat_sessions_TO_chat_messages"))
    private ChatSession session;
}
