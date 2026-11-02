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
@Table(name = "course_leg", uniqueConstraints = {
        @UniqueConstraint(name = "UQ_leg_course_day_order", columnNames = {"course_day_id", "leg_order"})
})
/** 코스 이동 구간 */
public class CourseLeg {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "course_leg_id", nullable = false)
    /** 이동구간ID */
    private Long courseLegId;

    @Column(name = "leg_order", nullable = false)
    /** 이동순서 */
    private Integer legOrder;

    @Column(name = "from_lodging_id")
    /** 출발숙소ID */
    private Long fromLodgingId;

    @Column(name = "to_lodging_id")
    /** 도착숙소ID */
    private Long toLodgingId;

    @Column(name = "transport_mode", nullable = false, length = 30)
    /** 이동수단 */
    private String transportMode;

    @Column(name = "distance_m")
    /** 이동거리 */
    private Integer distanceM;

    @Column(name = "duration_seconds")
    /** 이동시간 */
    private Integer durationSeconds;

    @Column(name = "route_geometry", columnDefinition = "LONGTEXT")
    /** 경로좌표 */
    private String routeGeometry;

    @Column(name = "route_source", nullable = false, length = 30)
    /** 경로출처 */
    private String routeSource;

    @Column(name = "calculated_at")
    /** 계산시각 */
    private LocalDateTime calculatedAt;

    @Column(name = "course_day_id", nullable = false)
    /** 코스 일자 */
    private Long courseDayId;

    @Column(name = "from_activity_id")
    /** 출발 활동 */
    private Long fromActivityId;

    @Column(name = "to_activity_id")
    /** 도착 활동 */
    private Long toActivityId;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "from_lodging_id", referencedColumnName = "lodging_id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "fk_lodging_to_course_leg"))
    private Lodging fromLodging;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "to_lodging_id", referencedColumnName = "lodging_id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "fk_lodging_to_course_leg2"))
    private Lodging toLodging;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_day_id", referencedColumnName = "id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "FK_course_leg_course_day_id"))
    private CourseDay courseDay;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumns(
            value = {
                    @JoinColumn(name = "from_activity_id", referencedColumnName = "id", insertable = false, updatable = false),
                    @JoinColumn(name = "course_day_id", referencedColumnName = "course_day_id", insertable = false, updatable = false)
            },
            foreignKey = @ForeignKey(name = "FK_leg_origin_same_day")
    )
    private Activity fromActivityAndCourseDay;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumns(
            value = {
                    @JoinColumn(name = "to_activity_id", referencedColumnName = "id", insertable = false, updatable = false),
                    @JoinColumn(name = "course_day_id", referencedColumnName = "course_day_id", insertable = false, updatable = false)
            },
            foreignKey = @ForeignKey(name = "FK_leg_destination_same_day")
    )
    private Activity toActivityAndCourseDay;
}
