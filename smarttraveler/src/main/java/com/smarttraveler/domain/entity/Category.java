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
@Table(name = "category", uniqueConstraints = {
        @UniqueConstraint(name = "UK_category_name", columnNames = {"name"})
})
/** 카테고리 */
public class Category {
    @Id
    @Column(name = "id", nullable = false)
    /** 카테고리 */
    private Integer id;

    @Column(name = "name", length = 20)
    /** 카테고리 이름 */
    private String name;
}
