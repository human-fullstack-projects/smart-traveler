package com.smarttraveler.chat;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.Map;

/** ID로 활성 숙소를 조회한다(예약 화면에서 선택한 숙소를 챗봇 추천에 연결). */
@Service
public class StoredLodgingCandidates {
    private static final String SELECT_LODGING = """
        SELECT l.lodging_id, l.name, l.address, l.region_id, l.latitude, l.longitude, r.full_name
        FROM lodging l LEFT JOIN region r ON r.region_id = l.region_id
        WHERE l.status='ACTIVE'
        """;
    private final JdbcTemplate jdbc;
    public StoredLodgingCandidates(JdbcTemplate jdbc) { this.jdbc = jdbc; }

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
}
