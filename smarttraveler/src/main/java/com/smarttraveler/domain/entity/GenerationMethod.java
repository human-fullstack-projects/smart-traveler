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
@Table(name = "generation_method", uniqueConstraints = {
        @UniqueConstraint(name = "UK_generation_method_code", columnNames = {"code"})
})
/** 생성방식 */
public class GenerationMethod {
    @Id
    @Column(name = "id", nullable = false)
    /** 방식 번호 */
    private Integer id;

    @Column(name = "code", length = 20)
    /** 방식 종류 */
    private String code;
}
