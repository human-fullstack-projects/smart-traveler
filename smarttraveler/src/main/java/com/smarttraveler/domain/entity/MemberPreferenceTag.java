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
@Table(name = "member_preference_tag")
@IdClass(MemberPreferenceTagId.class)
/** 회원 취향 */
public class MemberPreferenceTag {
    @Id
    @Column(name = "member_no", nullable = false)
    /** 회원번호 */
    private Long memberNo;

    @Id
    @Column(name = "tag_id", nullable = false)
    /** 취향 번호 */
    private Long tagId;

    @ColumnDefault("'like'")
    @Column(name = "polarity", nullable = false, columnDefinition = "ENUM('like','avoid')")
    /** 선호/기피 */
    private String polarity;

    @ColumnDefault("1")
    @Column(name = "weight", nullable = false)
    /** 강도 */
    private Integer weight;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_no", referencedColumnName = "member_id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "FK_member_preference_tag_member_no"))
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tag_id", referencedColumnName = "id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "FK_member_preference_tag_tag_id"))
    private PreferenceTag tag;
}
