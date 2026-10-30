package com.smarttraveler.admin.data;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.*;
import java.math.BigDecimal;

@Entity @Table(name = "owner_profiles") @Getter @Setter
public class OwnerProfile {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "owner_id") private Long ownerId;
    @Column(name = "member_id") private Long memberId;
    @Column(name = "business_number", length = 20) private String businessNumber;
    @Column(name = "business_name", length = 100) private String businessName;
    @Column(name = "representative_name", length = 50) private String representativeName;
    @Column(name = "business_address", length = 255) private String businessAddress;
    @Column(name = "business_phone", length = 20) private String businessPhone;
    @Column(name = "review_status_code", length = 20) private String reviewStatusCode;
    @Column(name = "created_at") private LocalDateTime createdAt;
    @Column(name = "updated_at") private LocalDateTime updatedAt;
}

