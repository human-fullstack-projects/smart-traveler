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
@Table(name = "recommendation_log")
/** 추천 기록 */
public class RecommendationLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    /** 추천 번호 */
    private Long id;

    @Column(name = "member_no", nullable = false)
    /** 회원번호 */
    private Long memberNo;

    @Column(name = "template_id", nullable = false)
    /** 템플릿 번호 */
    private Long templateId;

    @Column(name = "score", precision = 6, scale = 3)
    /** 유사도 점수 */
    private BigDecimal score;

    @Column(name = "algo_version", length = 50)
    /** 추천 방식 버전 */
    private String algoVersion;

    @Column(name = "shown_at")
    /** 노출일시 */
    private LocalDateTime shownAt;

    @Column(name = "adopted_course_id")
    /** 채택한 코스 번호 */
    private Long adoptedCourseId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_no", referencedColumnName = "member_id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "FK_recommendation_log_member_no"))
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "template_id", referencedColumnName = "id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "FK_recommendation_log_template_id"))
    private CourseTemplate template;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "adopted_course_id", referencedColumnName = "id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "FK_recommendation_log_adopted_course_id"))
    private Course adoptedCourse;
}
