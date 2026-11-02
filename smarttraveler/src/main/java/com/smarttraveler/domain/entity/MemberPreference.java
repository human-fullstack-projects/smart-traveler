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
@Table(name = "member_preference")
/** 회원 기본 설정 */
public class MemberPreference {
    @Id
    @Column(name = "member_no", nullable = false)
    /** 회원번호 */
    private Long memberNo;

    @ColumnDefault("'normal'")
    @Column(name = "pace", nullable = false, columnDefinition = "ENUM('relaxed','normal','packed')")
    /** 일정 강도 */
    private String pace;

    @ColumnDefault("'normal'")
    @Column(name = "wake_up", nullable = false, columnDefinition = "ENUM('early','normal','late')")
    /** 기상 시간대 */
    private String wakeUp;

    @Column(name = "updated_at")
    /** 수정날짜 */
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_no", referencedColumnName = "member_id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "FK_member_preference_member_no"))
    private Member member;
}
