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
@Table(name = "chat_sessions")
/** 대화 세션 */
public class ChatSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "session_id", nullable = false)
    /** 대화 세션 번호 */
    private Long sessionId;

    @Column(name = "member_id")
    /** 회원번호 */
    private Long memberId;

    @Column(name = "guest_token", length = 100)
    /** 비회원 식별값 */
    private String guestToken;

    @Column(name = "session_title", length = 100)
    /** 대화 제목 */
    private String sessionTitle;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at", nullable = false)
    /** 생성일시 */
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    /** 수정일시 */
    private LocalDateTime updatedAt;

    @Column(name = "course_id")
    /** 대상 코스 번호 */
    private Long courseId;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "member_id", referencedColumnName = "member_id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "FK_members_TO_chat_sessions"))
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "course_id", referencedColumnName = "id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "FK_chat_sessions_course_id"))
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumns(
            value = {
                    @JoinColumn(name = "course_id", referencedColumnName = "id", insertable = false, updatable = false),
                    @JoinColumn(name = "member_id", referencedColumnName = "member_no", insertable = false, updatable = false)
            },
            foreignKey = @ForeignKey(name = "FK_chat_course_owner")
    )
    private Course courseAndMember;
}
