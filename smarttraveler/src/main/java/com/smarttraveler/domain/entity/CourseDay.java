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
@Table(name = "course_day", uniqueConstraints = {
        @UniqueConstraint(name = "UK_course_day_course_id_day_no", columnNames = {"course_id", "day_no"})
})
/** 세부코스 */
public class CourseDay {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    /** 세부 코스 번호 */
    private Long id;

    @Column(name = "course_id", nullable = false)
    /** 코스 번호 */
    private Long courseId;

    @Column(name = "day_no", nullable = false)
    /** 일차 */
    private Integer dayNo;

    @Column(name = "theme", length = 200)
    /** 테마 */
    private String theme;

    @Column(name = "visit_date")
    /** 일정 날짜 */
    private LocalDate visitDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", referencedColumnName = "id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "FK_course_day_course_id"))
    private Course course;
}
