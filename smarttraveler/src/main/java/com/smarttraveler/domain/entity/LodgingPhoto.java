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
@Table(name = "lodging_photo")
/** 숙소 사진 */
public class LodgingPhoto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "photo_id", nullable = false)
    /** 사진 ID */
    private Long photoId;

    @Column(name = "lodging_id", nullable = false)
    /** 숙소 ID */
    private Long lodgingId;

    @Column(name = "image_url", nullable = false, length = 1000)
    /** 이미지 주소 */
    private String imageUrl;

    @Column(name = "thumbnail_url", length = 1000)
    /** 작은 이미지 주소 */
    private String thumbnailUrl;

    @Column(name = "copyright_type", length = 20)
    /** 저작권 유형 */
    private String copyrightType;

    @ColumnDefault("'OWNER'")
    @Column(name = "source_provider", nullable = false, length = 30)
    /** 데이터 출처 */
    private String sourceProvider;

    @ColumnDefault("1")
    @Column(name = "sort_order", nullable = false)
    /** 표시 순서 */
    private Integer sortOrder;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at", nullable = false)
    /** 생성일시 */
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lodging_id", referencedColumnName = "lodging_id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "FK_lodging_TO_lodging_photo_lodging_id"))
    private Lodging lodging;
}
