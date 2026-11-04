package com.smarttraveler.chat;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.Map;

/** 선택한 지역에 저장된 관광지만 후보로 전달한다. */
@Service
public class StoredPlaceCandidates {
    private final JdbcTemplate jdbc;
    public StoredPlaceCandidates(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public List<Map<String, Object>> find(ObjectNode conditions) {
        Long id = regionId(conditions);
        if (id == null) return List.of();
        return jdbc.query("SELECT p.place_id,p.name,COALESCE(p.address,'') FROM place p JOIN region r ON r.region_id=p.region_id " +
                        "WHERE (p.region_id=? OR r.parent_region_id=?) AND p.source_provider='TOURAPI' AND p.is_active=TRUE ORDER BY p.name,p.place_id LIMIT 30",
                (rs, row) -> Map.<String,Object>of("placeId", rs.getLong(1), "name", rs.getString(2), "address", rs.getString(3)), id, id);
    }
    private Long regionId(ObjectNode conditions) {
        var field = conditions.path("regionId");
        if (field.isIntegralNumber() && field.canConvertToLong() && field.asLong() > 0) return field.asLong();
        String name = conditions.path("regionName").asText("").trim();
        if (name.isEmpty() || name.length() > 100) return null;
        List<Long> exact = jdbc.query("SELECT region_id FROM region WHERE is_supported=TRUE AND abolished_at IS NULL AND (full_name=? OR region_name=?) LIMIT 2",
                (rs, row) -> rs.getLong(1), name, name);
        if (exact.size() == 1) return exact.get(0);
        if (!exact.isEmpty()) return null;
        // 시도 약칭이 하나로만 연결될 때만 후보를 찾는다.
        String prefix = name.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        List<Long> matches = jdbc.query("SELECT region_id FROM region WHERE parent_region_id IS NULL AND is_supported=TRUE AND abolished_at IS NULL AND region_name LIKE CONCAT(?, '%') LIMIT 2",
                (rs, row) -> rs.getLong(1), prefix);
        return matches.size() == 1 ? matches.get(0) : null;
    }
}
