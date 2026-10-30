package com.smarttraveler.admin.data;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.*;
import java.math.BigDecimal;

@Entity @Table(name = "course_template") @Getter @Setter
public class CourseTemplate {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "id") private Long id;
    @Column(name = "source_course_id") private Long sourceCourseId;
    @Column(name = "destination") private String destination;
    @Column(name = "duration_days") private Byte durationDays;
    @Column(name = "title") private String title;
    @Column(name = "summary") private String summary;
    @Column(name = "adopt_count") private Integer adoptCount;
    @Column(name = "like_count") private Integer likeCount;
    @Column(name = "feedback_count") private Integer feedbackCount;
    @Column(name = "is_active") private Boolean isActive;
    @Column(name = "created_at") private LocalDateTime createdAt;
}

