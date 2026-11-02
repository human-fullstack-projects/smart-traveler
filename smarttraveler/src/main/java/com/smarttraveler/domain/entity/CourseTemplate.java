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
@Table(name = "course_template", uniqueConstraints = {
        @UniqueConstraint(name = "UK_course_template_source_course_id", columnNames = {"source_course_id"})
})
/** 추천용 코스 템플릿 */
public class CourseTemplate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    /** 템플릿 번호 */
    private Long id;

    @Column(name = "source_course_id")
    /** 원본 코스 번호 */
    private Long sourceCourseId;

    @Column(name = "destination", nullable = false, length = 100)
    /** 목적지 */
    private String destination;

    @Column(name = "duration_days", nullable = false)
    /** 일수 */
    private Integer durationDays;

    @Column(name = "season", columnDefinition = "ENUM('spring','summer','autumn','winter')")
    /** 계절 */
    private String season;

    @Column(name = "party_type", columnDefinition = "ENUM('solo','couple','friends','family_with_kids','family_adults')")
    /** 동행 유형 */
    private String partyType;

    @Column(name = "pace", nullable = false, columnDefinition = "ENUM('relaxed','normal','packed')")
    /** 일정 강도 */
    private String pace;

    @Column(name = "title", nullable = false, length = 200)
    /** 제목 */
    private String title;

    @Column(name = "summary", columnDefinition = "TEXT")
    /** 요약 */
    private String summary;

    @ColumnDefault("0")
    @Column(name = "adopt_count", nullable = false)
    /** 채택 수 */
    private Integer adoptCount;

    @ColumnDefault("0")
    @Column(name = "like_count", nullable = false)
    /** 좋아요 수 */
    private Integer likeCount;

    @ColumnDefault("0")
    @Column(name = "feedback_count", nullable = false)
    /** 피드백 수 */
    private Integer feedbackCount;

    @ColumnDefault("true")
    @Column(name = "is_active", nullable = false)
    /** 활성화 상태 */
    private Boolean isActive;

    @Column(name = "last_verified_at")
    /** 장소 확인일 */
    private LocalDateTime lastVerifiedAt;

    @Column(name = "created_at")
    /** 생성날짜 */
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "source_course_id", referencedColumnName = "id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "FK_course_template_source_course_id"))
    private Course sourceCourse;
}
