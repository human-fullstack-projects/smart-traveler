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
@Table(name = "holiday")
/** 휴일 */
public class Holiday {
    @Id
    @Column(name = "holiday_date", nullable = false)
    /** 날짜 */
    private LocalDate holidayDate;

    @ColumnDefault("TRUE")
    @Column(name = "is_holiday", nullable = false)
    /** 쉬는 날 여부 */
    private Boolean isHoliday;
}
