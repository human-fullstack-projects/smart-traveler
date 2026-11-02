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
@Table(name = "human_course_photo")
/** 휴먼 코스 사진 */
public class HumanCoursePhoto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "photo_id", nullable = false)
    /** 사진 번호 */
    private Long photoId;

    @Column(name = "post_id", nullable = false)
    /** 게시글 번호 */
    private Long postId;

    @Column(name = "image_url", nullable = false, length = 1000)
    /** 사진 URL */
    private String imageUrl;

    @Column(name = "sort_order", nullable = false)
    /** 정렬 순서 */
    private Integer sortOrder;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at", nullable = false)
    /** 생성일시 */
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", referencedColumnName = "post_id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "FK_human_course_photo_post_id"))
    private HumanCoursePost post;
}
