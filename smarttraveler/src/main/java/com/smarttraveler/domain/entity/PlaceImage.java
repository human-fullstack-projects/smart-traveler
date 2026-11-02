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
@Table(name = "place_image")
/** 장소 이미지 */
public class PlaceImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "place_image_id", nullable = false)
    /** 이미지ID */
    private Long placeImageId;

    @Column(name = "place_id", nullable = false)
    /** 장소ID */
    private Long placeId;

    @Column(name = "image_url", nullable = false, length = 1000)
    /** 이미지주소 */
    private String imageUrl;

    @Column(name = "image_type", length = 30)
    /** 이미지유형 */
    private String imageType;

    @Column(name = "sort_order", nullable = false)
    /** 표시순서 */
    private Integer sortOrder;

    @Column(name = "source_provider", length = 30)
    /** 데이터출처 */
    private String sourceProvider;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at", nullable = false)
    /** 생성일시 */
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "place_id", referencedColumnName = "place_id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "fk_place_to_place_image"))
    private Place place;
}
