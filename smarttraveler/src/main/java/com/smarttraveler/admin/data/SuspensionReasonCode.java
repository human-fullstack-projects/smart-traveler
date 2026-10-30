package com.smarttraveler.admin.data;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity @Table(name = "suspension_reason_codes") @Getter @Setter
public class SuspensionReasonCode {
    @Id @Column(name = "reason_code", length = 20) private String reasonCode;
    @Column(name = "reason_name") private String reasonName;
}

