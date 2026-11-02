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
@Table(name = "terms")
/** 약관 */
public class Term {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "terms_id", nullable = false)
    /** 약관번호 */
    private Long termsId;

    @Column(name = "terms_type", nullable = false, length = 30)
    /** 약관 종류 */
    private String termsType;

    @Column(name = "terms_version", nullable = false, length = 20)
    /** 약관 버전 */
    private String termsVersion;

    @Column(name = "is_required", nullable = false)
    /** 필수 여부 */
    private Boolean isRequired;

    @Column(name = "effective_date", nullable = false)
    /** 시행일 */
    private LocalDate effectiveDate;

    @Column(name = "content", columnDefinition = "TEXT")
    /** 약관 내용 */
    private String content;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at", nullable = false)
    /** 생성일시 */
    private LocalDateTime createdAt;
}
