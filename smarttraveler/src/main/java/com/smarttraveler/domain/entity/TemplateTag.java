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
@Table(name = "template_tag")
@IdClass(TemplateTagId.class)
/** 템플릿 태그 */
public class TemplateTag {
    @Id
    @Column(name = "template_id", nullable = false)
    /** 템플릿 번호 */
    private Long templateId;

    @Id
    @Column(name = "tag_id", nullable = false)
    /** 취향 번호 */
    private Long tagId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "template_id", referencedColumnName = "id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "FK_template_tag_template_id"))
    private CourseTemplate template;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tag_id", referencedColumnName = "id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "FK_template_tag_tag_id"))
    private PreferenceTag tag;
}
