package com.smarttraveler.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "admin_lodging_action_history")
public class AdminLodgingActionHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "lodging_id", nullable = false)
    private Long lodgingId;
    @Column(name = "actor_login", nullable = false, length = 50)
    private String actorLogin;
    @Column(name = "action", nullable = false, length = 30)
    private String action;
    @Column(name = "reason", length = 500)
    private String reason;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected AdminLodgingActionHistory() {}
    public AdminLodgingActionHistory(Long lodgingId, String actorLogin, String action, String reason) {
        this.lodgingId = lodgingId; this.actorLogin = actorLogin; this.action = action;
        this.reason = reason; this.createdAt = LocalDateTime.now();
    }
}
