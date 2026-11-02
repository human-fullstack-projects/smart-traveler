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
@Table(name = "room_photo")
/** 객실사진 */
public class RoomPhoto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "photo_id", nullable = false)
    /** 사진 번호 */
    private Long photoId;

    @Column(name = "room_id", nullable = false)
    /** 객실 번호 */
    private Long roomId;

    @Column(name = "image_url", nullable = false, length = 1000)
    /** 사진 URL */
    private String imageUrl;

    @Column(name = "thumbnail_url", length = 1000)
    /** 썸네일 URL */
    private String thumbnailUrl;

    @Column(name = "copyright_type", length = 20)
    /** 저작권 유형 */
    private String copyrightType;

    @Column(name = "source_provider", nullable = false, length = 30)
    /** 제공처 */
    private String sourceProvider;

    @Column(name = "sort_order", nullable = false)
    /** 정렬 순서 */
    private Integer sortOrder;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at", nullable = false)
    /** 생성일시 */
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", referencedColumnName = "room_id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "FK_room_photo_room_id"))
    private Room room;
}
