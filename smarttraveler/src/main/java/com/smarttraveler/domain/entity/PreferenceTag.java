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
@Table(name = "preference_tag", uniqueConstraints = {
        @UniqueConstraint(name = "UK_preference_tag_tag_type_name", columnNames = {"tag_type", "name"})
})
/** 취향 태그 */
public class PreferenceTag {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    /** 취향 번호 */
    private Long id;

    @Column(name = "tag_type", nullable = false, columnDefinition = "ENUM('interest','food','transport','style')")
    /** 취향 종류 */
    private String tagType;

    @Column(name = "name", nullable = false, length = 50)
    /** 취향 이름 */
    private String name;
}
