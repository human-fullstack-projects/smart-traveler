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
@Table(name = "members", uniqueConstraints = {
        @UniqueConstraint(name = "UK_members2", columnNames = {"email"}),
        @UniqueConstraint(name = "UK_members3", columnNames = {"login_id"})
})
/** 회원 */
public class Member {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id", nullable = false)
    /** 회원번호 */
    private Long memberId;

    @Column(name = "login_id", nullable = false, length = 50)
    /** 로그인 아이디 */
    private String loginId;

    @Column(name = "password", nullable = false, length = 255)
    /** 비밀번호 */
    private String password;

    @Column(name = "nickname", length = 50)
    /** 닉네임 */
    private String nickname;

    @Column(name = "email", length = 255)
    /** 이메일 */
    private String email;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at", nullable = false)
    /** 가입일시 */
    private LocalDateTime createdAt;

    @ColumnDefault("CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    @Column(name = "updated_at", nullable = false)
    /** 수정일시 */
    private LocalDateTime updatedAt;

    @ColumnDefault("'ACTIVE'")
    @Column(name = "status", nullable = false, length = 30)
    /** 회원상태 */
    private String status;

    @ColumnDefault("'USER'")
    @Column(name = "role_code", nullable = false, length = 20)
    /** 회원 역할 코드 */
    private String roleCode;

    @Column(name = "deleted_at")
    /** 탈퇴일시 */
    private LocalDateTime deletedAt;
}
