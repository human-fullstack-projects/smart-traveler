package com.smarttraveler.admin.data;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity @Table(name = "admin_human_featured") @Getter @Setter
public class HumanFeatured {
    @Id @Column(name = "post_id") private Long postId;
    @Column(name = "featured", nullable = false) private Boolean featured;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;
}
