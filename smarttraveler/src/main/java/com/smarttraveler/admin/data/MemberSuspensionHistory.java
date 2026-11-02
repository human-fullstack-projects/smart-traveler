package com.smarttraveler.admin.data;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.*;
import java.math.BigDecimal;

@Entity(name = "AdminMemberSuspensionHistory") @Table(name = "member_suspension_history") @Getter @Setter
public class MemberSuspensionHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "suspension_id") private Long suspensionId;
    @Column(name = "member_id") private Long memberId;
    @Column(name = "reason_code", length = 20) private String reasonCode;
    @Column(name = "reason_detail", length = 500) private String reasonDetail;
    @Column(name = "started_at") private LocalDateTime startedAt;
    @Column(name = "suspended_until") private LocalDateTime suspendedUntil;
    @Column(name = "released_at") private LocalDateTime releasedAt;
    @Column(name = "processed_by") private Long processedBy;
    @Column(name = "created_at") private LocalDateTime createdAt;
}

