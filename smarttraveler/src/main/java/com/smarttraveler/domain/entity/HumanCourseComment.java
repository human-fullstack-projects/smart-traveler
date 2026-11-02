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
@Table(name = "human_course_comment")
/** 휴먼 코스 댓글 */
public class HumanCourseComment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id", nullable = false)
    /** 댓글 번호 */
    private Long commentId;

    @Column(name = "post_id", nullable = false)
    /** 게시글 번호 */
    private Long postId;

    @Column(name = "author_id", nullable = false)
    /** 작성자 */
    private Long authorId;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    /** 댓글 내용 */
    private String content;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at", nullable = false)
    /** 생성일시 */
    private LocalDateTime createdAt;

    @ColumnDefault("CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    @Column(name = "updated_at", nullable = false)
    /** 수정일시 */
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    /** 삭제일시 */
    private LocalDateTime deletedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", referencedColumnName = "post_id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "FK_human_course_comment_post_id"))
    private HumanCoursePost post;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", referencedColumnName = "member_id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "FK_human_course_comment_author_id"))
    private Member author;
}
