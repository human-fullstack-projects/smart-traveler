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
@Table(name = "template_activity", uniqueConstraints = {
        @UniqueConstraint(name = "UK_template_activity_template_day_id_seq", columnNames = {"template_day_id", "seq"})
})
/** 템플릿 활동 */
public class TemplateActivity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    /** 템플릿 활동 번호 */
    private Long id;

    @Column(name = "template_day_id", nullable = false)
    /** 템플릿 일자 번호 */
    private Long templateDayId;

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
    /** 카테고리 */
    private Integer categoryId;

    @Column(name = "place_name", nullable = false, length = 200)
    /** 장소이름 */
    private String placeName;

    @Column(name = "place_id")
    /** 장소번호 */
    private Long placeId;

    @Column(name = "reason", length = 300)
    /** 추천이유 */
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "template_day_id", referencedColumnName = "id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "FK_template_activity_template_day_id"))
    private TemplateDay templateDay;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", referencedColumnName = "id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "FK_template_activity_category_id"))
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "place_id", referencedColumnName = "place_id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "FK_template_activity_place_id"))
    private Place place;
}
