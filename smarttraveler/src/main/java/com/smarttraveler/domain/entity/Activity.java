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
@Table(name = "activity", uniqueConstraints = {
        @UniqueConstraint(name = "UK_activity_course_day_id_seq", columnNames = {"course_day_id", "seq"}),
        @UniqueConstraint(name = "UQ_activity_id_day", columnNames = {"course_day_id", "id"})
})
/** 활동 내역 */
public class Activity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    /** 활동내역 번호 */
    private Long id;

    @Column(name = "course_day_id", nullable = false)
    /** 세부코스 번호 */
    private Long courseDayId;

    @Column(name = "seq", nullable = false)
    /** 순서 */
    private Integer seq;

    @Column(name = "start_time", nullable = false)
    /** 시작시간 */
    private LocalTime startTime;

    @Column(name = "end_time")
    /** 종료시간 */
    private LocalTime endTime;

    @Column(name = "title", nullable = false, length = 200)
    /** 제목 */
    private String title;

    @Column(name = "category_id", nullable = false)
    /** 카테고리 id */
    private Integer categoryId;

    @Column(name = "place_name", nullable = false, length = 200)
    /** 장소이름 */
    private String placeName;

    @Column(name = "place_id")
    /** 장소번호 */
    private Long placeId;

    @Column(name = "note", columnDefinition = "TEXT")
    /** 추가설명 */
    private String note;

    @Column(name = "reason", length = 300)
    /** 추천이유 */
    private String reason;

    @Column(name = "address_snapshot", length = 300)
    /** 방문 장소 주소 */
    private String addressSnapshot;

    @Column(name = "latitude_snapshot", precision = 10, scale = 7)
    /** 방문 위도 */
    private BigDecimal latitudeSnapshot;

    @Column(name = "longitude_snapshot", precision = 10, scale = 7)
    /** 방문 경도 */
    private BigDecimal longitudeSnapshot;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_day_id", referencedColumnName = "id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "FK_activity_course_day_id"))
    private CourseDay courseDay;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", referencedColumnName = "id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "FK_activity_category_id"))
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "place_id", referencedColumnName = "place_id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "FK_activity_place_id"))
    private Place place;
}
