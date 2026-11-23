package com.smarttraveler.chat;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

/** 이름으로 활성 숙소를 찾는다(챗봇의 "숙소 이름으로 코스 추천" 입력용). */
@Service
public class StoredLodgingCandidates {
    private static final String SELECT_LODGING = """
        SELECT l.lodging_id, l.name, l.address, l.region_id, l.latitude, l.longitude, r.full_name
        FROM lodging l LEFT JOIN region r ON r.region_id = l.region_id
        WHERE l.status='ACTIVE'
        """;
    private final JdbcTemplate jdbc;
    public StoredLodgingCandidates(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<Map<String, Object>> findByName(String name) {
        return jdbc.query(SELECT_LODGING + "AND l.name LIKE ? ORDER BY l.name, l.lodging_id LIMIT 5",
                StoredLodgingCandidates::map, "%" + like(name.trim()) + "%");
    }

    public Map<String, Object> findById(long lodgingId) {
        var rows = jdbc.query(SELECT_LODGING + "AND l.lodging_id=?", StoredLodgingCandidates::map, lodgingId);
        return rows.isEmpty() ? null : rows.getFirst();
    }

    private static Map<String, Object> map(java.sql.ResultSet rs, int row) throws java.sql.SQLException {
        Map<String, Object> lodging = new java.util.HashMap<>();
        lodging.put("lodgingId", rs.getLong(1));
        lodging.put("name", rs.getString(2));
        lodging.put("address", rs.getString(3) == null ? "" : rs.getString(3));
        lodging.put("regionId", rs.getObject(4, Long.class));
        lodging.put("latitude", rs.getBigDecimal(5));
        lodging.put("longitude", rs.getBigDecimal(6));
        lodging.put("regionName", rs.getString(7));
        return lodging;
    }

    private static String like(String s) { return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_"); }
}
