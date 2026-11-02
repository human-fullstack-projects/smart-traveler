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
@Table(name = "owner_review_status")
/** 심사 상태 코드 */
public class OwnerReviewStatus {
    @Id
    @Column(name = "review_status_code", length = 20)
    /** 심사 상태 코드 */
    private String reviewStatusCode;

    @Column(name = "review_status_name", nullable = false, length = 50)
    /** 심사 상태명 */
    private String reviewStatusName;
}
