package com.smarttraveler.mypage.profile.entity;

import com.smarttraveler.domain.entity.Member;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Table(name = "mypage_member_profile_image")
public class MemberProfileImage {
    @Id @Column(name = "member_id")
    private Long memberId;
    @Column(name = "avatar_data", nullable = false, columnDefinition = "LONGTEXT")
    private String avatarData;
    @Column(name = "updated_at", nullable = false)
    @ColumnDefault("CURRENT_TIMESTAMP")
    private LocalDateTime updatedAt;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", insertable = false, updatable = false,
        foreignKey = @ForeignKey(name = "fk_mypage_profile_image_member"))
    private Member member;
}
