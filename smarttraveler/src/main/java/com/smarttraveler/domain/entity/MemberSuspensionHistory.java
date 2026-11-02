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
@Table(name = "member_suspension_history")
/** 회원 정지 이력 */
public class MemberSuspensionHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "suspension_id", nullable = false)
    /** 정지 번호 */
    private Long suspensionId;

    @Column(name = "member_id", nullable = false)
    /** 회원번호 */
    private Long memberId;

    @Column(name = "reason_code", nullable = false, length = 20)
    /** 정지 사유 코드 */
    private String reasonCode;

    @Column(name = "reason_detail", length = 500)
    /** 정지 상세 사유 */
    private String reasonDetail;

    @Column(name = "started_at", nullable = false)
    /** 정지 시작일시 */
    private LocalDateTime startedAt;

    @Column(name = "suspended_until")
    /** 정지 해제 예정일시 */
    private LocalDateTime suspendedUntil;

    @Column(name = "released_at")
    /** 실제 해제일시 */
    private LocalDateTime releasedAt;

    @Column(name = "processed_by", nullable = false)
    /** 처리자 회원번호 */
    private Long processedBy;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at", nullable = false)
    /** 생성일시 */
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", referencedColumnName = "member_id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "FK_members_TO_member_suspension_history"))
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "processed_by", referencedColumnName = "member_id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "FK_members_TO_member_suspension_history2"))
    private Member processedByMembers;
}
