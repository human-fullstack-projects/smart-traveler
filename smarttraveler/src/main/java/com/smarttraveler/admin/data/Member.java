package com.smarttraveler.admin.data;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity(name = "AdminMember") @Table(name = "members") @Getter @Setter
public class Member {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "member_id") private Long memberId;
    @Column(name = "login_id", length = 50) private String loginId;
    @Column(length = 255) private String password;
    @Column(length = 50) private String nickname;
    @Column(length = 255) private String email;
    @Column(name = "created_at") private LocalDateTime createdAt;
    @Column(name = "updated_at") private LocalDateTime updatedAt;
    @Column(length = 30) private String status;
    @Column(name = "role_code", length = 20) private String roleCode;
    @Column(name = "deleted_at") private LocalDateTime deletedAt;
}
