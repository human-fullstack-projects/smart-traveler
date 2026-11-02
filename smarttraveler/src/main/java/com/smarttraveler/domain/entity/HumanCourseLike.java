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
@Table(name = "human_course_like")
@IdClass(HumanCourseLikeId.class)
/** 휴먼 추천 코스 좋아요 */
public class HumanCourseLike {
    @Id
    @Column(name = "post_id", nullable = false)
    /** 게시글 */
    private Long postId;

    @Id
    @Column(name = "member_id", nullable = false)
    /** 회원 */
    private Long memberId;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at", nullable = false)
    /** 생성일시 */
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", referencedColumnName = "post_id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "FK_human_course_like_post_id"))
    private HumanCoursePost post;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", referencedColumnName = "member_id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "FK_human_course_like_member_id"))
    private Member member;
}
