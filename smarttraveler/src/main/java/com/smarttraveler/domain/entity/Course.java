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
@Table(name = "course", uniqueConstraints = {
        @UniqueConstraint(name = "UQ_course_id_member", columnNames = {"id", "member_no"})
})
/** 코스 */
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    /** 코스 번호 */
    private Long id;

    @Column(name = "member_no", nullable = false)
    /** 회원번호 */
    private Long memberNo;

    @Column(name = "destination", nullable = false, length = 100)
    /** 목적지 */
    private String destination;

    @Column(name = "start_date", nullable = false)
    /** 시작날짜 */
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    /** 종료날짜 */
    private LocalDate endDate;

    @ColumnDefault("1")
    @Column(name = "adults", nullable = false)
    /** 성인 */
    private Integer adults;

    @ColumnDefault("0")
    @Column(name = "children", nullable = false)
    /** 아동수 */
    private Integer children;

    @ColumnDefault("'[]'")
    @Column(name = "fixed_events", nullable = false, columnDefinition = "JSON")
    /** 고정 일정 */
    private String fixedEvents;

    @Column(name = "preferences_snapshot", nullable = false, columnDefinition = "JSON")
    /** 생성 당시 취향 */
    private String preferencesSnapshot;

    @Column(name = "title", length = 200)
    /** 제목 */
    private String title;

    @Column(name = "summary", columnDefinition = "TEXT")
    /** 요약 */
    private String summary;

    @ColumnDefault("'[]'")
    @Column(name = "tips", nullable = false, columnDefinition = "JSON")
    /** 팁 */
    private String tips;

    @ColumnDefault("'[]'")
    @Column(name = "assumptions", nullable = false, columnDefinition = "JSON")
    /** 가정 사항 */
    private String assumptions;

    @Column(name = "revision_request", length = 500)
    /** 수정요청내용 */
    private String revisionRequest;

    @Column(name = "origin_template_id")
    /** 원본 템플릿 번호 */
    private Long originTemplateId;

    @ColumnDefault("false")
    @Column(name = "share_consent", nullable = false)
    /** 공유 동의 */
    private Boolean shareConsent;

    @Column(name = "generation_method_id", nullable = false)
    /** 생성방식 번호 */
    private Integer generationMethodId;

    @ColumnDefault("'queued'")
    @Column(name = "status", nullable = false, columnDefinition = "ENUM('queued','running','done','failed')")
    /** 생성 상태 */
    private String status;

    @Column(name = "error_message", length = 1000)
    /** 오류 메시지 */
    private String errorMessage;

    @Column(name = "model", length = 100)
    /** 모델명 */
    private String model;

    @Column(name = "prompt_version", length = 50)
    /** 프롬프트버전 */
    private String promptVersion;

    @ColumnDefault("true")
    @Column(name = "is_active", nullable = false)
    /** 활성화 상태 */
    private Boolean isActive;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at", nullable = false)
    /** 생성날짜 */
    private LocalDateTime createdAt;

    @ColumnDefault("CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    @Column(name = "updated_at", nullable = false)
    /** 수정날짜 */
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    /** 삭제날짜 */
    private LocalDateTime deletedAt;

    @Column(name = "companion_type", length = 30)
    /** 동행 유형 */
    private String companionType;

    @Column(name = "transport_type", length = 30)
    /** 교통수단 */
    private String transportType;

    @Column(name = "flow_type", length = 20)
    /** 여행 시작 방식 */
    private String flowType;

    @Column(name = "condition_state", length = 20)
    /** 조건 확인 상태 */
    private String conditionState;

    @Column(name = "condition_meta", columnDefinition = "LONGTEXT")
    /** 조건 정보 */
    private String conditionMeta;

    @Column(name = "conditions_confirmed_at")
    /** 조건 확인일시 */
    private LocalDateTime conditionsConfirmedAt;

    @Column(name = "plan_state", length = 20)
    /** 여행 상태 */
    private String planState;

    @ColumnDefault("0")
    @Column(name = "lock_version", nullable = false)
    /** 동시 수정 버전 */
    private Integer lockVersion;

    @Column(name = "region_code", length = 30)
    /** 지역 코드 */
    private String regionCode;

    @Column(name = "region_id")
    /** 여행 지역 */
    private Long regionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_no", referencedColumnName = "member_id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "FK_course_member_no"))
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "origin_template_id", referencedColumnName = "id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "FK_course_origin_template_id"))
    private CourseTemplate originTemplate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "generation_method_id", referencedColumnName = "id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "FK_course_generation_method_id"))
    private GenerationMethod generationMethod;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "region_id", referencedColumnName = "region_id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "FK_course_region_id"))
    private Region region;
}
