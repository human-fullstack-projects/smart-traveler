package com.smarttraveler.admin.data;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity @Table(name = "member_role_codes") @Getter @Setter
public class MemberRoleCode {
    @Id @Column(name = "role_code", length = 20) private String roleCode;
    @Column(name = "role_name") private String roleName;
}

