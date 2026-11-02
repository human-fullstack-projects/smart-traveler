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
@Table(name = "human_course_report", uniqueConstraints = {
        @UniqueConstraint(name = "UQ_human_report_post_reporter", columnNames = {"post_id", "reporter_id"})
})
/** 휴먼 추천 코스 신고 */
public class HumanCourseReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id", nullable = false)
    /** 신고 번호 */
    private Long reportId;

    @Column(name = "post_id", nullable = false)
    /** 게시글 */
    private Long postId;

    @Column(name = "reporter_id", nullable = false)
    /** 신고자 */
    private Long reporterId;

    @Column(name = "reason", nullable = false, length = 500)
    /** 신고 사유 */
    private String reason;

    @ColumnDefault("'PENDING'")
    @Column(name = "status", nullable = false, length = 20)
    /** 처리 상태 */
    private String status;

    @Column(name = "processed_by")
    /** 처리자 */
    private Long processedBy;

    @Column(name = "processed_at")
    /** 처리 시각 */
    private LocalDateTime processedAt;

    @Column(name = "resolution", length = 500)
    /** 처리 내용 */
    private String resolution;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at", nullable = false)
    /** 생성일시 */
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", referencedColumnName = "post_id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "FK_human_course_report_post_id"))
    private HumanCoursePost post;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reporter_id", referencedColumnName = "member_id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "FK_human_course_report_reporter_id"))
    private Member reporter;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "processed_by", referencedColumnName = "member_id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "FK_human_course_report_processed_by"))
    private Member processedByMembers;
}
