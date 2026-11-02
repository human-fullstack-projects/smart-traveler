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
@Table(name = "member_consents")
/** 회원 약관 동의 */
public class MemberConsent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "consent_id", nullable = false)
    /** 동의 이력 번호 */
    private Long consentId;

    @Column(name = "member_id", nullable = false)
    /** 회원번호 */
    private Long memberId;

    @Column(name = "is_agreed", nullable = false)
    /** 동의 여부 */
    private Boolean isAgreed;

    @Column(name = "consented_at", nullable = false)
    /** 동의 처리일시 */
    private LocalDateTime consentedAt;

    @Column(name = "terms_id", nullable = false)
    /** 약관번호 */
    private Long termsId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", referencedColumnName = "member_id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "FK_members_TO_member_consents"))
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "terms_id", referencedColumnName = "terms_id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "FK_terms_TO_member_consents"))
    private Term terms;
}
