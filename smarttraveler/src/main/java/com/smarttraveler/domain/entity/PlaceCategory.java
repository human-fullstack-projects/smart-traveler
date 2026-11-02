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
@Table(name = "place_category")
/** 장소 분류 */
public class PlaceCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id", nullable = false)
    /** 분류ID */
    private Long categoryId;

    @Column(name = "parent_category_id")
    /** 상위분류ID */
    private Long parentCategoryId;

    @Column(name = "category_name", nullable = false, length = 100)
    /** 분류명 */
    private String categoryName;

    @Column(name = "category_code", nullable = false, length = 50)
    /** 분류코드 */
    private String categoryCode;

    @Column(name = "is_active", nullable = false)
    /** 사용여부 */
    private Boolean isActive;

    @Column(name = "category_path", length = 200)
    /** 분류 경로 */
    private String categoryPath;

    @Column(name = "depth")
    /** 분류 깊이 */
    private Integer depth;

    @Column(name = "group_code", length = 3)
    /** 카카오 그룹 코드 */
    private String groupCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "parent_category_id", referencedColumnName = "category_id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "FK_place_category_parent_category_id"))
    private PlaceCategory parentCategory;
}
