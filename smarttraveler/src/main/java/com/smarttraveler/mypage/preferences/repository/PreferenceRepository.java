package com.smarttraveler.mypage.preferences.repository;

import com.smarttraveler.mypage.preferences.dto.PreferenceDtos.*;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PreferenceRepository {
    private final JdbcTemplate jdbc;
    public PreferenceRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public List<TagOption> tags() {
        return jdbc.query("SELECT tag_type, name FROM preference_tag ORDER BY tag_type, id",
            (rs, n) -> new TagOption(rs.getString("tag_type"), rs.getString("name")));
    }
    public Preferences find(long owner, List<TagOption> catalog) {
        var settings = jdbc.queryForList("SELECT pace, wake_up FROM member_preference WHERE member_no=?", owner);
        var tags = jdbc.query("""
            SELECT t.tag_type, t.name, p.polarity, p.weight FROM member_preference_tag p
            JOIN preference_tag t ON t.id=p.tag_id WHERE p.member_no=? ORDER BY t.tag_type, t.id
            """, (rs, n) -> new Selection(rs.getString("tag_type"), rs.getString("name"),
                rs.getString("polarity"), rs.getInt("weight")), owner);
        return new Preferences(owner, settings.isEmpty() ? "normal" : settings.getFirst().get("pace").toString(),
            settings.isEmpty() ? "normal" : settings.getFirst().get("wake_up").toString(), tags, catalog, !settings.isEmpty());
    }
    public boolean lockOwner(long owner) {
        return !jdbc.queryForList("SELECT member_id FROM members WHERE member_id=? AND status='ACTIVE' AND deleted_at IS NULL FOR UPDATE", Long.class, owner).isEmpty();
    }
    public void save(long owner, Save settings) {
        jdbc.update("""
            INSERT INTO member_preference(member_no, pace, wake_up, updated_at) VALUES (?, ?, ?, CURRENT_TIMESTAMP)
            ON DUPLICATE KEY UPDATE pace=VALUES(pace), wake_up=VALUES(wake_up), updated_at=CURRENT_TIMESTAMP
            """, owner, settings.pace(), settings.wakeUp());
        jdbc.update("DELETE FROM member_preference_tag WHERE member_no=?", owner);
        // 전 회원이 같은 순서로 태그 유일키를 잡아 동시 저장 시 잠금 충돌을 줄인다.
        var ordered = settings.tags().stream().sorted(java.util.Comparator.comparing(Selection::type).thenComparing(Selection::name)).toList();
        for (var tag : ordered) {
            jdbc.update("INSERT INTO preference_tag(tag_type, name) VALUES (?, ?) ON DUPLICATE KEY UPDATE name=VALUES(name)", tag.type(), tag.name());
            Long tagId = jdbc.queryForObject("SELECT id FROM preference_tag WHERE tag_type=? AND name=?", Long.class, tag.type(), tag.name());
            jdbc.update("INSERT INTO member_preference_tag(member_no, tag_id, polarity, weight) VALUES (?, ?, ?, ?)", owner, tagId, tag.polarity(), tag.weight());
        }
    }
}
