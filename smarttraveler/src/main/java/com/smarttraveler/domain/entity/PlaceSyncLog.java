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
@Table(name = "place_sync_log")
/** 장소 동기화 이력 */
public class PlaceSyncLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sync_id", nullable = false)
    /** 동기화 번호 */
    private Long syncId;

    @Column(name = "place_id", nullable = false)
    /** 장소 */
    private Long placeId;

    @Column(name = "synced_at", nullable = false)
    /** 동기화 시각 */
    private LocalDateTime syncedAt;

    @Column(name = "result", nullable = false, length = 20)
    /** 결과 */
    private String result;

    @Column(name = "changed_fields", length = 500)
    /** 변경 항목 */
    private String changedFields;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "place_id", referencedColumnName = "place_id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "FK_place_sync_log_place_id"))
    private Place place;
}
