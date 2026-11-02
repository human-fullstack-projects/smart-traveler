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
@Table(name = "data_sync_log")
/** 데이터 수집 이력 */
public class DataSyncLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sync_log_id", nullable = false)
    /** 수집이력ID */
    private Long syncLogId;

    @Column(name = "region_id")
    /** 지역ID */
    private Long regionId;

    @Column(name = "provider", nullable = false, length = 30)
    /** 제공처 */
    private String provider;

    @Column(name = "data_type", nullable = false, length = 30)
    /** 데이터유형 */
    private String dataType;

    @Column(name = "status", nullable = false, length = 20)
    /** 상태 */
    private String status;

    @Column(name = "started_at", nullable = false)
    /** 시작시각 */
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    /** 종료시각 */
    private LocalDateTime finishedAt;

    @Column(name = "total_count")
    /** 조회건수 */
    private Integer totalCount;

    @Column(name = "success_count")
    /** 성공건수 */
    private Integer successCount;

    @Column(name = "failure_count")
    /** 실패건수 */
    private Integer failureCount;

    @Column(name = "error_message", columnDefinition = "TEXT")
    /** 오류내용 */
    private String errorMessage;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "region_id", referencedColumnName = "region_id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "fk_region_to_data_sync_log"))
    private Region region;
}
