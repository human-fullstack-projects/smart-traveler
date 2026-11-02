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
@Table(name = "template_day", uniqueConstraints = {
        @UniqueConstraint(name = "UK_template_day_template_id_day_no", columnNames = {"template_id", "day_no"})
})
/** 템플릿 일자 */
public class TemplateDay {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    /** 템플릿 일자 번호 */
    private Long id;

    @Column(name = "template_id", nullable = false)
    /** 템플릿 번호 */
    private Long templateId;

    @Column(name = "day_no", nullable = false)
    /** 일차 */
    private Integer dayNo;

    @Column(name = "theme", length = 200)
    /** 테마 */
    private String theme;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "template_id", referencedColumnName = "id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "FK_template_day_template_id"))
    private CourseTemplate template;
}
