package com.smarttraveler.admin.data;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity @Table(name = "account_status_codes") @Getter @Setter
public class AccountStatusCode {
    @Id @Column(name = "status_code", length = 20) private String statusCode;
    @Column(name = "status_name") private String statusName;
}

