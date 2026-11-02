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
@Table(name = "chat_course_draft", uniqueConstraints = {
        @UniqueConstraint(name = "UQ_chat_course_draft_session_id", columnNames = {"session_id"})
})
/** 대화 중 코스 임시안 */
public class ChatCourseDraft {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "draft_id", nullable = false)
    /** 임시안 번호 */
    private Long draftId;

    @Column(name = "session_id", nullable = false)
    /** 대화 세션 */
    private Long sessionId;

    @Column(name = "draft_payload", nullable = false, columnDefinition = "JSON")
    /** 코스 임시안 JSON */
    private String draftPayload;

    @Column(name = "expires_at", nullable = false)
    /** 만료 시각 */
    private LocalDateTime expiresAt;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at", nullable = false)
    /** 생성일시 */
    private LocalDateTime createdAt;

    @ColumnDefault("CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    @Column(name = "updated_at", nullable = false)
    /** 갱신 일시 */
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", referencedColumnName = "session_id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "FK_chat_course_draft_session_id"))
    private ChatSession session;
}
