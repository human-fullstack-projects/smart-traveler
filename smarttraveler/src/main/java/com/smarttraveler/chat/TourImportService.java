package com.smarttraveler.chat;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.sql.Statement;
import java.util.List;
import java.util.Objects;

/** 관광공사 코드와 기존 지역·장소 ID를 연결해 조회한 항목을 저장한다. */
@Service
public class TourImportService {
    private final JdbcTemplate jdbc;
    public TourImportService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public void save(ObjectNode data) {
        JsonNode province = data.path("province"), district = data.path("district"), items = data.path("items");
        String provinceCode = code(province.path("code")), provinceName = required(province.path("name"), 100);
        if (!items.isArray()) throw new IllegalArgumentException("관광지 목록 형식이 올바르지 않습니다.");
        long provinceId = region("TOUR_SIDO", provinceCode, provinceName, provinceName, null);
        Long districtId = null;
        if (district.isObject()) {
            String districtCode = code(district.path("code")), districtName = required(district.path("name"), 100);
            districtId = region("TOUR_SIGUNGU", provinceCode + ":" + districtCode, districtName, provinceName + " " + districtName, provinceId);
        }
        data.put("regionId", districtId == null ? provinceId : districtId);
        for (JsonNode item : items) {
            if (!(item instanceof ObjectNode object)) throw new IllegalArgumentException("관광지 형식이 올바르지 않습니다.");
            String sourceId = required(item.path("id"), 100), name = required(item.path("name"), 200);
            String areaCode = item.path("districtCode").asText("").trim();
            long regionId = provinceId;
            boolean hasDistrict = !areaCode.isEmpty();
            if (hasDistrict) {
                if (!areaCode.matches("[0-9]{1,5}")) throw new IllegalArgumentException("시군구 코드가 올바르지 않습니다.");
                String areaName = required(item.path("districtName"), 100);
                regionId = region("TOUR_SIGUNGU", provinceCode + ":" + areaCode, areaName, provinceName + " " + areaName, provinceId);
            }
            String address = optional(item.path("address"), 300), phone = optional(item.path("phone"), 30);
            BigDecimal latitude = coordinate(item.path("latitude"), 90), longitude = coordinate(item.path("longitude"), 180);
            // 중복 자료는 API 식별자로 갱신하고 수동 상태·설명·이미지는 건드리지 않는다.
            jdbc.update("INSERT INTO place (region_id,source_provider,source_place_id,place_type,name,address,latitude,longitude,phone,is_active,verification_status,last_synced_at,updated_at) " +
                    "VALUES (?, 'TOURAPI', ?, 'ATTRACTION', ?, ?, ?, ?, ?, TRUE, 'UNVERIFIED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) " +
                    "ON DUPLICATE KEY UPDATE region_id=IF(?,VALUES(region_id),region_id),name=VALUES(name),address=COALESCE(VALUES(address),address),latitude=COALESCE(VALUES(latitude),latitude),longitude=COALESCE(VALUES(longitude),longitude),phone=COALESCE(VALUES(phone),phone),last_synced_at=CURRENT_TIMESTAMP,updated_at=CURRENT_TIMESTAMP",
                    regionId, sourceId, name, address, latitude, longitude, phone, hasDistrict);
            Long placeId = jdbc.queryForObject("SELECT place_id FROM place WHERE source_provider='TOURAPI' AND source_place_id=?", Long.class, sourceId);
            object.put("placeId", Objects.requireNonNull(placeId));
        }
    }
    private long region(String provider, String external, String name, String fullName, Long parent) {
        // 외부 코드 행을 먼저 잠가 동시 요청 시 같은 지역을 두 번 만들지 않는다.
        jdbc.update("INSERT INTO region_external_code (provider,external_code,region_id) VALUES (?,?,NULL) ON DUPLICATE KEY UPDATE external_code=VALUES(external_code)", provider, external);
        List<Long> mapped = jdbc.query("SELECT region_id FROM region_external_code WHERE provider=? AND external_code=? FOR UPDATE", (rs, row) -> (Long) rs.getObject(1), provider, external);
        if (mapped.size() != 1) throw new IllegalStateException("지역 매핑을 확인할 수 없습니다.");
        if (mapped.get(0) != null) return mapped.get(0);
        List<Long> matches = parent == null
                ? jdbc.query("SELECT region_id FROM region WHERE parent_region_id IS NULL AND full_name=?", (rs, row) -> rs.getLong(1), fullName)
                : jdbc.query("SELECT region_id FROM region WHERE parent_region_id=? AND full_name=?", (rs, row) -> rs.getLong(1), parent, fullName);
        if (matches.size() > 1) throw new IllegalStateException("같은 이름의 기존 지역이 여러 개입니다.");
        long id;
        if (matches.size() == 1) id = matches.get(0);
        else {
            var key = new GeneratedKeyHolder();
            jdbc.update(connection -> {
                var statement = connection.prepareStatement("INSERT INTO region (region_code,region_name,full_name,region_level,parent_region_id,is_supported,updated_at) VALUES (?,?,?,?,?,TRUE,CURRENT_TIMESTAMP)", Statement.RETURN_GENERATED_KEYS);
                statement.setString(1, "KTO-" + external.replace(':', '-')); statement.setString(2, name); statement.setString(3, fullName);
                statement.setString(4, parent == null ? "CITY" : "DISTRICT");
                if (parent == null) statement.setNull(5, java.sql.Types.BIGINT); else statement.setLong(5, parent);
                return statement;
            }, key);
            id = Objects.requireNonNull(key.getKey()).longValue();
        }
        jdbc.update("UPDATE region_external_code SET region_id=? WHERE provider=? AND external_code=?", id, provider, external);
        return id;
    }
    private static String required(JsonNode node, int max) {
        if (!node.isTextual()) throw new IllegalArgumentException("관광정보 필수 항목이 없습니다.");
        String value = node.asText().trim();
        if (value.isEmpty() || value.length() > max) throw new IllegalArgumentException("관광정보 필수 항목이 올바르지 않습니다.");
        return value;
    }
    private static String optional(JsonNode node, int max) {
        if (!node.isTextual() || node.asText().isBlank()) return null;
        String value = node.asText().trim();
        return value.length() <= max ? value : value.substring(0, max);
    }
    private static String code(JsonNode node) {
        String value = required(node, 5);
        if (!value.matches("[0-9]{1,5}")) throw new IllegalArgumentException("지역 코드가 올바르지 않습니다.");
        return value;
    }
    private static BigDecimal coordinate(JsonNode value, int maximum) {
        try {
            BigDecimal decimal = new BigDecimal(value.asText(""));
            if (decimal.signum() == 0 || decimal.abs().compareTo(BigDecimal.valueOf(maximum)) > 0) return null;
            return decimal;
        } catch (NumberFormatException ex) { return null; }
    }
}
