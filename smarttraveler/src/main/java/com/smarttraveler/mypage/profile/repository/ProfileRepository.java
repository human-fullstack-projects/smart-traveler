package com.smarttraveler.mypage.profile.repository;

import com.smarttraveler.mypage.profile.dto.ProfileDtos.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ProfileRepository {
    private final JdbcTemplate jdbc;
    public ProfileRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public Profile find(long owner) {
        return jdbc.queryForObject("""
            SELECT m.member_id, m.login_id, m.nickname, m.email, p.avatar_data
            FROM members m LEFT JOIN mypage_member_profile_image p ON p.member_id=m.member_id
            WHERE m.member_id=? AND m.status='ACTIVE' AND m.deleted_at IS NULL
            """, (rs, n) -> new Profile(rs.getLong("member_id"), rs.getString("login_id"),
                rs.getString("nickname"), rs.getString("email"), rs.getString("avatar_data")), owner);
    }

    public String lockPassword(long owner) {
        var rows = jdbc.queryForList("SELECT password FROM members WHERE member_id=? AND status='ACTIVE' AND deleted_at IS NULL FOR UPDATE", String.class, owner);
        return rows.isEmpty() ? null : rows.getFirst();
    }

    public void update(long owner, String nickname, String email, String encodedPassword, String avatarData) {
        if (encodedPassword == null) {
            jdbc.update("UPDATE members SET nickname=?, email=?, updated_at=CURRENT_TIMESTAMP WHERE member_id=?", nickname, email, owner);
        } else {
            jdbc.update("UPDATE members SET nickname=?, email=?, password=?, updated_at=CURRENT_TIMESTAMP WHERE member_id=?", nickname, email, encodedPassword, owner);
        }
        if (avatarData == null) {
            jdbc.update("DELETE FROM mypage_member_profile_image WHERE member_id=?", owner);
        } else {
            jdbc.update("""
                INSERT INTO mypage_member_profile_image(member_id, avatar_data, updated_at) VALUES (?, ?, CURRENT_TIMESTAMP)
                ON DUPLICATE KEY UPDATE avatar_data=VALUES(avatar_data), updated_at=CURRENT_TIMESTAMP
                """, owner, avatarData);
        }
    }
}
