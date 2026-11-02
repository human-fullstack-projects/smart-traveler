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
@Table(name = "restaurant_detail")
/** 식당 상세 */
public class RestaurantDetail {
    @Id
    @Column(name = "place_id", nullable = false)
    /** 장소 번호 */
    private Long placeId;

    @Column(name = "food_type", length = 50)
    /** 음식 유형 */
    private String foodType;

    @Column(name = "food_subtype", length = 50)
    /** 음식 세부 유형 */
    private String foodSubtype;

    @Column(name = "franchise_name", length = 100)
    /** 브랜드명 */
    private String franchiseName;

    @Column(name = "main_menu", length = 300)
    /** 주요 메뉴 */
    private String mainMenu;

    @Column(name = "price_range", length = 20)
    /** 가격대 */
    private String priceRange;

    @Column(name = "reservable")
    /** 예약 가능 */
    private Boolean reservable;

    @Column(name = "parking")
    /** 주차 가능 */
    private Boolean parking;

    @Column(name = "takeout")
    /** 포장 가능 */
    private Boolean takeout;

    @Column(name = "group_ok")
    /** 단체 가능 */
    private Boolean groupOk;

    @Column(name = "break_time", length = 100)
    /** 브레이크 타임 */
    private String breakTime;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at", nullable = false)
    /** 생성일시 */
    private LocalDateTime createdAt;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "updated_at", nullable = false)
    /** 수정일시 */
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "place_id", referencedColumnName = "place_id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "FK_restaurant_detail_place_id"))
    private Place place;
}
