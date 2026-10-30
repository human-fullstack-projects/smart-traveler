package com.smarttraveler.admin.data;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.*;
import java.math.BigDecimal;

@Entity @Table(name = "admin_human_course") @Getter @Setter
public class HumanCourse {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "id") private Long id;
    @Column(name = "member_id") private Long memberId;
    @Column(name = "title") private String title;
    @Column(name = "destination") private String destination;
    @Column(name = "summary") private String summary;
    @Column(name = "status") private String status;
    @Column(name = "featured") private Boolean featured;
    @Column(name = "created_at") private LocalDateTime createdAt;
}

