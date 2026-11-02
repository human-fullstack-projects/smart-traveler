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
@Table(name = "place_category_mapping")
@IdClass(PlaceCategoryMappingId.class)
/** 장소 분류 연결 */
public class PlaceCategoryMapping {
    @Id
    @Column(name = "place_id", nullable = false)
    /** 장소ID */
    private Long placeId;

    @Id
    @Column(name = "category_id", nullable = false)
    /** 분류ID */
    private Long categoryId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", referencedColumnName = "category_id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "fk_place_category_to_place_category_mapping"))
    private PlaceCategory category;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "place_id", referencedColumnName = "place_id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "fk_place_to_place_category_mapping"))
    private Place place;
}
