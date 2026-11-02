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
@Table(name = "lodging_peak_season")
/** 숙소 성수기 기간 */
public class LodgingPeakSeason {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "season_id", nullable = false)
    /** 성수기 ID */
    private Long seasonId;

    @Column(name = "lodging_id", nullable = false)
    /** 숙소 ID */
    private Long lodgingId;

    @Column(name = "start_date", nullable = false)
    /** 시작일 */
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    /** 종료일 */
    private LocalDate endDate;

    @Column(name = "name", length = 50)
    /** 기간 이름 */
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lodging_id", referencedColumnName = "lodging_id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "FK_lodging_TO_lodging_peak_season_lodging_id"))
    private Lodging lodging;
}
