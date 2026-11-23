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
                        "WHERE (p.region_id=? OR r.parent_region_id=?) AND p.source_provider IN ('TOURAPI','KTO_TOURAPI') AND p.is_active=TRUE ORDER BY p.name,p.place_id LIMIT 30",
                (rs, row) -> Map.<String,Object>of("placeId", rs.getLong(1), "name", rs.getString(2), "address", rs.getString(3)), id, id);
    }

    /** 숙소 좌표 주변(약 ±0.3도, 30km대)의 활성 관광지를 거리순으로 최대 30개 돌려준다. 기존 코스가 없을 때만 쓴다. */
    public List<Map<String, Object>> findNearLodging(java.math.BigDecimal lat, java.math.BigDecimal lng) {
        double range = 0.3;
        double centerLat = lat.doubleValue(), centerLng = lng.doubleValue();
        List<Map<String, Object>> rows = jdbc.query("SELECT place_id,name,COALESCE(address,''),latitude,longitude FROM place " +
                        "WHERE source_provider IN ('TOURAPI','KTO_TOURAPI') AND is_active=TRUE " +
                        "AND latitude BETWEEN ? AND ? AND longitude BETWEEN ? AND ? LIMIT 60",
                (rs, row) -> new java.util.HashMap<String, Object>(Map.of("placeId", rs.getLong(1), "name", rs.getString(2),
                        "address", rs.getString(3), "latitude", rs.getDouble(4), "longitude", rs.getDouble(5))),
                centerLat - range, centerLat + range, centerLng - range, centerLng + range);
        rows.sort(java.util.Comparator.comparingDouble(row ->
                roughDistanceKm(centerLat, centerLng, (Double) row.get("latitude"), (Double) row.get("longitude"))));
        return rows.size() > 30 ? rows.subList(0, 30) : rows;
    }

    /** 정밀한 Haversine 공식 대신 평면 근사식으로 거리를 어림한다(정렬용으로만 충분). */
    static double roughDistanceKm(double lat1, double lng1, double lat2, double lng2) {
        double dLat = (lat1 - lat2) * 111.0;
        double dLng = (lng1 - lng2) * 88.8;
        return Math.sqrt(dLat * dLat + dLng * dLng);
    }
    Long regionId(ObjectNode conditions) {
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
