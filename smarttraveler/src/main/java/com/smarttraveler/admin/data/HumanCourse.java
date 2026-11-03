package com.smarttraveler.admin.data;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.*;
import java.math.BigDecimal;

@Entity @Table(name = "human_course_post") @Getter @Setter
public class HumanCourse {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "post_id") private Long id;
    @Column(name = "author_id") private Long memberId;
    @Column(name = "course_id") private Long courseId;
    @Column(name = "reservation_id") private Long reservationId;
    @Column(name = "title", nullable = false, length = 100) private String title;
    @Column(name = "content", columnDefinition = "TEXT") private String content;
    @Column(name = "course_snapshot", columnDefinition = "LONGTEXT") private String courseSnapshot;
    @Column(name = "status", nullable = false, length = 20) private String status;
    @Transient private String destination;
    @Transient private Boolean featured;
    @Column(name = "created_at") private LocalDateTime createdAt;
    @Column(name = "updated_at") private LocalDateTime updatedAt;
    @Column(name = "deleted_at") private LocalDateTime deletedAt;
    public String getSummary() { return content; }
}

