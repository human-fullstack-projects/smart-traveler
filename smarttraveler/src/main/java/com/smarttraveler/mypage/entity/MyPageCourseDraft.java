package com.smarttraveler.mypage.entity;

import com.smarttraveler.domain.entity.Member;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.hibernate.annotations.ColumnDefault;

/** 생성·예약이 확정되지 않은 코스. 기존 course의 완료 상태와 혼용하지 않는다. */
@Entity
@Table(name = "mypage_course_draft", uniqueConstraints = {
    @UniqueConstraint(name = "uq_mypage_draft_owner_client", columnNames = {"member_id", "client_id"})
})
public class MyPageCourseDraft {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "member_id", nullable = false)
    private Long memberId;
    @Column(name = "client_id", nullable = false, length = 64)
    private String clientId;
    @Column(nullable = false, length = 200)
    private String title;
    @Column(nullable = false, length = 100)
    private String region;
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;
    @Column(name = "days_json", nullable = false, columnDefinition = "JSON")
    private String daysJson;
    @Column(name = "created_at", nullable = false)
    @ColumnDefault("CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", insertable = false, updatable = false,
        foreignKey = @ForeignKey(name = "fk_mypage_draft_member"))
    private Member member;
}
