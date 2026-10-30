package com.smarttraveler.admin.data;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity @Table(name = "generation_method") @Getter @Setter
public class GenerationMethod {
    @Id private Byte id;
    private String code;
}

