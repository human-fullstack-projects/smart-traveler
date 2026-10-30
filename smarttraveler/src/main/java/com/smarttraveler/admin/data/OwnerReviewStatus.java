package com.smarttraveler.admin.data;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity @Table(name = "owner_review_status") @Getter @Setter
public class OwnerReviewStatus {
    @Id @Column(name = "review_status_code", length = 20) private String reviewStatusCode;
    @Column(name = "review_status_name") private String reviewStatusName;
}

