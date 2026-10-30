package com.smarttraveler.admin.data;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.*;
import java.math.BigDecimal;

@Entity @Table(name = "lodging") @Getter @Setter
public class Lodging {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "lodging_id") private Long lodgingId;
    @Column(name = "owner_id") private Long ownerId;
    @Column(name = "source_provider", length = 30) private String sourceProvider;
    @Column(name = "source_content_id", length = 100) private String sourceContentId;
    @Column(name = "lodging_type", length = 30) private String lodgingType;
    @Column(name = "name", length = 200) private String name;
    @Column(name = "address", length = 300) private String address;
    @Column(name = "address_detail", length = 200) private String addressDetail;
    @Column(name = "zipcode", length = 10) private String zipcode;
    @Column(name = "latitude", precision = 10, scale = 7) private BigDecimal latitude;
    @Column(name = "longitude", precision = 10, scale = 7) private BigDecimal longitude;
    @Column(name = "phone", length = 30) private String phone;
    @Column(name = "homepage_url", length = 1000) private String homepageUrl;
    @Column(name = "description", columnDefinition = "TEXT") private String description;
    @Column(name = "checkin_time") private LocalTime checkinTime;
    @Column(name = "checkout_time") private LocalTime checkoutTime;
    @Column(name = "parking_info", length = 200) private String parkingInfo;
    @Column(name = "amenities", length = 500) private String amenities;
    @Column(name = "refund_policy", columnDefinition = "TEXT") private String refundPolicy;
    @Column(name = "status", length = 20) private String status;
    @Column(name = "reject_reason", length = 500) private String rejectReason;
    @Column(name = "approved_at") private LocalDateTime approvedAt;
    @Column(name = "created_at") private LocalDateTime createdAt;
    @Column(name = "updated_at") private LocalDateTime updatedAt;
}

