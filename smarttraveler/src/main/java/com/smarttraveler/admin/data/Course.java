package com.smarttraveler.admin.data;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.*;
import java.math.BigDecimal;

@Entity @Table(name = "course") @Getter @Setter
public class Course {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "id") private Long id;
    @Column(name = "member_no") private Long memberNo;
    @Column(name = "destination") private String destination;
    @Column(name = "start_date") private LocalDate startDate;
    @Column(name = "end_date") private LocalDate endDate;
    private Byte adults;
    private Byte children;
    @Column(name = "fixed_events", columnDefinition = "json") private String fixedEvents;
    @Column(name = "preferences_snapshot", columnDefinition = "json") private String preferencesSnapshot;
    @Column(name = "tips", columnDefinition = "json") private String tips;
    @Column(name = "assumptions", columnDefinition = "json") private String assumptions;
    @Column(name = "title") private String title;
    @Column(name = "summary") private String summary;
    @Column(name = "parent_course_id") private Long parentCourseId;
    @Column(name = "revision_request") private String revisionRequest;
    @Column(name = "origin_template_id") private Long originTemplateId;
    @Column(name = "share_consent") private Boolean shareConsent;
    @Column(name = "generation_method_id") private Byte generationMethodId;
    @Column(name = "status") private String status;
    @Column(name = "error_message") private String errorMessage;
    @Column(name = "model") private String model;
    @Column(name = "prompt_version") private String promptVersion;
    @Column(name = "is_active") private Boolean isActive;
    @Column(name = "created_at") private LocalDateTime createdAt;
    @Column(name = "updated_at") private LocalDateTime updatedAt;
    @Column(name = "deleted_at") private LocalDateTime deletedAt;
}

