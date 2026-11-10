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
@Table(name = "human_course_post", uniqueConstraints = {
        @UniqueConstraint(name = "UQ_human_course_post_reservation_id", columnNames = {"reservation_id"})
})
/** 휴먼 추천 코스 */
public class HumanCoursePost {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_id", nullable = false)
    /** 게시글 번호 */
    private Long postId;

    @Column(name = "reservation_id", nullable = false)
    /** 객실예약 번호 */
    private Long reservationId;

    @Column(name = "author_id", nullable = false)
    /** 작성자 */
    private Long authorId;

    @Column(name = "course_id", nullable = false)
    /** 코스 번호 */
    private Long courseId;

    @Column(name = "title", nullable = false, length = 100)
    /** 제목 */
    private String title;

    @Column(name = "category", length = 30)
    private String category;

    @Column(name = "summary", length = 150)
    private String summary;

    @Column(name = "companion_type", length = 30)
    /** 동행 유형 */
    private String companionType;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    /** 여행 이야기 */
    private String content;

    @Column(name = "status", nullable = false, length = 20)
    /** 게시 상태 */
    private String status;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at", nullable = false)
    /** 생성일시 */
    private LocalDateTime createdAt;

    @ColumnDefault("CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    @Column(name = "updated_at", nullable = false)
    /** 수정일시 */
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    /** 삭제일시 */
    private LocalDateTime deletedAt;

    @Column(name = "course_snapshot", nullable = false, columnDefinition = "LONGTEXT")
    /** 게시 당시 코스 스냅샷 */
    private String courseSnapshot;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reservation_id", referencedColumnName = "reservation_id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "FK_human_course_post_reservation_id"))
    private RoomReservation reservation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", referencedColumnName = "member_id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "FK_human_course_post_author_id"))
    private Member author;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", referencedColumnName = "id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "FK_human_course_post_course_id"))
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumns(
            value = {
                    @JoinColumn(name = "reservation_id", referencedColumnName = "reservation_id", insertable = false, updatable = false),
                    @JoinColumn(name = "author_id", referencedColumnName = "member_id", insertable = false, updatable = false)
            },
            foreignKey = @ForeignKey(name = "FK_human_post_reservation_owner")
    )
    private RoomReservation reservationAndAuthor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumns(
            value = {
                    @JoinColumn(name = "reservation_id", referencedColumnName = "reservation_id", insertable = false, updatable = false),
                    @JoinColumn(name = "course_id", referencedColumnName = "course_id", insertable = false, updatable = false)
            },
            foreignKey = @ForeignKey(name = "FK_human_post_reservation_course")
    )
    private RoomReservation reservationAndCourse;
}
