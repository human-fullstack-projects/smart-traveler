package com.smarttraveler.tour.service;

import com.smarttraveler.tour.client.TourApiClient;
import com.smarttraveler.tour.client.TourApiClient.Page;
import com.smarttraveler.tour.client.TourApiClient.TourApiException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

/**
 * 관광지 데이터 수집 (WBS 「관광지 연관·중심 정보 연동」 EXT-01·EXT-07 + TourAPI 좌표·사진).
 * 지우지 않고 upsert 만 한다. 한 단계가 실패해도 기존 데이터는 그대로 두고 data_sync_log 에 「미반영」으로 남긴다.
 */
@Service
public class TourSyncService {
    static final String P_TOUR = "KTO_TOURAPI";
    static final String P_HUB = "KTO_HUB";
    static final String P_RELATED = "KTO_RELATED";
    private static final DateTimeFormatter YM = DateTimeFormatter.ofPattern("yyyyMM");
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final TourApiClient api;
    private final JdbcTemplate jdbc;
    /** 이름 비교 때 떼어 낼 지역 이름 (예: 경주). sync 시작 때 정한다. */
    private volatile String regionShort = "";

    public TourSyncService(TourApiClient api, JdbcTemplate jdbc) {
        this.api = api;
        this.jdbc = jdbc;
    }

    /** This tour-only table is supplied by sample-data/tour_schema.sql, not by the team entities. */
    private void ensureHubTable() {
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS place_hub_rank (
              hub_rank_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
              region_id BIGINT NOT NULL,
              base_ym CHAR(6) NOT NULL,
              hub_code VARCHAR(100) NOT NULL,
              hub_name VARCHAR(200) NOT NULL,
              hub_rank INT NOT NULL,
              category_large VARCHAR(50) NULL,
              category_middle VARCHAR(50) NULL,
              place_id BIGINT NULL,
              synced_at DATETIME NOT NULL,
              UNIQUE KEY uq_hub (region_id, base_ym, hub_code),
              CONSTRAINT fk_hub_region FOREIGN KEY (region_id) REFERENCES region(region_id),
              CONSTRAINT fk_hub_place FOREIGN KEY (place_id) REFERENCES place(place_id)
            )
            """);
    }

    public record StepResult(String provider, String dataType, String status, int total, int success, String message) {}
    public record SyncResult(String regionName, String baseYm, List<StepResult> steps) {}

    public List<Map<String, Object>> supportedRegions() {
        return jdbc.queryForList("SELECT region_id, region_code, full_name FROM region WHERE region_level='SIGUNGU' AND is_supported=TRUE ORDER BY region_code");
    }

    /** regionCode = 시군구 5자리 (예: 47130 경주시). */
    public SyncResult sync(String regionCode) {
        ensureHubTable();
        Map<String, Object> region = jdbc.queryForMap("SELECT region_id, full_name FROM region WHERE region_code=?", regionCode);
        long regionId = ((Number) region.get("region_id")).longValue();
        String fullName = (String) region.get("full_name");
        regionShort = fullName.substring(fullName.lastIndexOf(' ') + 1).replaceAll("[시군구]$", "");
        String areaCd = regionCode.substring(0, 2);
        String signguTail = regionCode.substring(2);
        List<StepResult> steps = new ArrayList<>();

        steps.add(syncAttractions(regionId, areaCd, signguTail));
        String baseYm = latestBaseYm(areaCd, regionCode);
        if (baseYm == null) {
            steps.add(log(regionId, P_HUB, "HUB_RANK", "FAILED", 0, 0, "최근 6개월 기준 연월 자료 없음 또는 호출 실패 · 미반영"));
            steps.add(log(regionId, P_RELATED, "RELATED", "SKIPPED", 0, 0, "기준 연월을 찾지 못해 건너뜀 · 미반영"));
        } else {
            steps.add(syncHub(regionId, baseYm, areaCd, regionCode));
            steps.add(syncRelated(regionId, baseYm, areaCd, regionCode));
        }
        return new SyncResult((String) region.get("full_name"), baseYm, steps);
    }

    /** TourAPI 관광지 → place / place_image (좌표·주소·사진). */
    StepResult syncAttractions(long regionId, String areaCd, String signguTail) {
        LocalDateTime started = LocalDateTime.now();
        int total = 0, ok = 0;
        try {
            for (int pageNo = 1; pageNo <= 10; pageNo++) {
                Page page = api.attractionPage(areaCd, signguTail, pageNo, 100);
                total = page.totalCount();
                for (JsonNode it : page.items()) {
                    long placeId = upsertPlace(regionId, P_TOUR, text(it, "contentid"), text(it, "title"),
                            text(it, "addr1"), text(it, "addr2"), decimal(it, "mapy"), decimal(it, "mapx"),
                            text(it, "tel"), modified(text(it, "modifiedtime")));
                    upsertImage(placeId, text(it, "firstimage"), "MAIN", 0);
                    upsertImage(placeId, text(it, "firstimage2"), "THUMB", 1);
                    ok++;
                }
                if (pageNo * 100 >= total || page.items().isEmpty()) break;
            }
            return log(regionId, P_TOUR, "ATTRACTION", "SUCCESS", total, ok, null, started);
        } catch (TourApiException e) {
            return log(regionId, P_TOUR, "ATTRACTION", "FAILED", total, ok, e.getMessage() + " · 기존 데이터 유지(미반영)", started);
        }
    }

    /** EXT-07 중심 관광지 순위 → place_hub_rank (+ TourAPI 장소와 이름으로 연결, 없으면 좌표 포함 장소로 추가). */
    StepResult syncHub(long regionId, String baseYm, String areaCd, String regionCode) {
        LocalDateTime started = LocalDateTime.now();
        int total = 0, ok = 0;
        try {
            Map<String, Long> byName = placeNames(regionId);
            for (int pageNo = 1; pageNo <= 5; pageNo++) {
                Page page = api.hubPage(baseYm, areaCd, regionCode, pageNo, 100);
                total = page.totalCount();
                for (JsonNode it : page.items()) {
                    String name = text(it, "hubTatsNm");
                    Long placeId = byName.get(norm(name));
                    if (placeId == null) {
                        placeId = upsertPlace(regionId, P_HUB, text(it, "hubTatsCd"), name, null, null,
                                decimal(it, "mapY"), decimal(it, "mapX"), null, null);
                        byName.put(norm(name), placeId);
                    }
                    jdbc.update("""
                            INSERT INTO place_hub_rank(region_id, base_ym, hub_code, hub_name, hub_rank, category_large, category_middle, place_id, synced_at)
                            VALUES (?,?,?,?,?,?,?,?,NOW())
                            ON DUPLICATE KEY UPDATE hub_name=VALUES(hub_name), hub_rank=VALUES(hub_rank), category_large=VALUES(category_large),
                              category_middle=VALUES(category_middle), place_id=VALUES(place_id), synced_at=NOW()""",
                            regionId, baseYm, text(it, "hubTatsCd"), name, intOf(it, "hubRank"),
                            text(it, "hubCtgryLclsNm"), text(it, "hubCtgryMclsNm"), placeId);
                    ok++;
                }
                if (pageNo * 100 >= total || page.items().isEmpty()) break;
            }
            // TourAPI 장소와 이름이 맞게 된 중심 관광지 전용 장소는 사용 안 함으로 표시 (지우지 않음)
            jdbc.update("""
                    UPDATE place p SET p.is_active=FALSE, p.updated_at=NOW()
                    WHERE p.region_id=? AND p.source_provider=? AND p.is_active=TRUE
                      AND NOT EXISTS (SELECT 1 FROM place_hub_rank h WHERE h.place_id=p.place_id)""", regionId, P_HUB);
            return log(regionId, P_HUB, "HUB_RANK", "SUCCESS", total, ok, "기준 연월 " + baseYm, started);
        } catch (TourApiException e) {
            return log(regionId, P_HUB, "HUB_RANK", "FAILED", total, ok, e.getMessage() + " · 기존 순위 유지(미반영)", started);
        }
    }

    /** EXT-01 관광지별 연관 관광지 → 팀 related_attraction_rank. */
    StepResult syncRelated(long regionId, String baseYm, String areaCd, String regionCode) {
        LocalDateTime started = LocalDateTime.now();
        int total = 0, ok = 0;
        try {
            Map<String, Long> byName = placeNames(regionId);
            for (int pageNo = 1; pageNo <= 5; pageNo++) {
                Page page = api.relatedPage(baseYm, areaCd, regionCode, pageNo, 1000);
                total = page.totalCount();
                for (JsonNode it : page.items()) {
                    String source = text(it, "tAtsNm");
                    jdbc.update("""
                            INSERT INTO related_attraction_rank(base_ym, attraction_code, attraction_name,
                              area_code, sigungu_code, related_code, related_name, related_region_code,
                              related_region_name, related_sigungu_code, related_sigungu_name,
                              category_large, category_medium, category_small, related_rank,
                              fetched_at, base_place_id, related_place_id, source_record_hash)
                            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,SHA2(CONCAT_WS('|',?,?,?),256))
                            ON DUPLICATE KEY UPDATE attraction_name=VALUES(attraction_name), related_name=VALUES(related_name),
                              related_region_name=VALUES(related_region_name), related_sigungu_name=VALUES(related_sigungu_name),
                              category_large=VALUES(category_large), category_medium=VALUES(category_medium),
                              category_small=VALUES(category_small), related_rank=VALUES(related_rank),
                              base_place_id=VALUES(base_place_id), related_place_id=VALUES(related_place_id), fetched_at=NOW()""",
                            baseYm, text(it, "tAtsCd"), source, areaCd, text(it, "signguCd"),
                            text(it, "rlteTatsCd"), text(it, "rlteTatsNm"), text(it, "rlteRegnCd"),
                            text(it, "rlteRegnNm"), text(it, "rlteSignguCd"), text(it, "rlteSignguNm"),
                            text(it, "rlteCtgryLclsNm"), text(it, "rlteCtgryMclsNm"), text(it, "rlteCtgrySclsNm"),
                            intOf(it, "rlteRank"), LocalDateTime.now(), byName.get(norm(source)),
                            byName.get(norm(text(it, "rlteTatsNm"))), baseYm, text(it, "tAtsCd"), text(it, "rlteTatsCd"));
                    ok++;
                }
                if (pageNo * 1000 >= total || page.items().isEmpty()) break;
            }
            return log(regionId, P_RELATED, "RELATED", "SUCCESS", total, ok, "기준 연월 " + baseYm, started);
        } catch (TourApiException e) {
            return log(regionId, P_RELATED, "RELATED", "FAILED", total, ok, e.getMessage() + " · 기존 연관 정보 유지(미반영)", started);
        }
    }

    /** 지난달부터 거꾸로 6개월 안에서 자료가 있는 첫 기준 연월 (EXT-07 1건 조회로 확인). */
    String latestBaseYm(String areaCd, String regionCode) {
        YearMonth ym = YearMonth.now().minusMonths(1);
        for (int i = 0; i < 6; i++, ym = ym.minusMonths(1)) {
            try {
                if (api.hubPage(ym.format(YM), areaCd, regionCode, 1, 1).totalCount() > 0) return ym.format(YM);
            } catch (TourApiException e) {
                return null;
            }
        }
        return null;
    }

    // ---- 화면용 조회 ----

    public List<Map<String, Object>> recentLogs() {
        return jdbc.queryForList("""
                SELECT l.*, r.full_name FROM data_sync_log l LEFT JOIN region r ON r.region_id=l.region_id
                ORDER BY l.sync_log_id DESC LIMIT 12""");
    }

    public Map<String, Object> counts(String regionCode) {
        ensureHubTable();
        return jdbc.queryForMap("""
                SELECT (SELECT COUNT(*) FROM place p JOIN region r ON r.region_id=p.region_id WHERE r.region_code=?) AS places,
                       (SELECT COUNT(*) FROM place p JOIN region r ON r.region_id=p.region_id WHERE r.region_code=? AND p.latitude IS NOT NULL) AS withCoords,
                       (SELECT COUNT(*) FROM place_image i JOIN place p ON p.place_id=i.place_id JOIN region r ON r.region_id=p.region_id WHERE r.region_code=? AND i.sort_order=0) AS withImage,
                       (SELECT COUNT(*) FROM place_hub_rank h JOIN region r ON r.region_id=h.region_id WHERE r.region_code=?) AS hubRanks,
                       (SELECT COUNT(*) FROM related_attraction_rank x JOIN place p ON p.place_id=x.base_place_id JOIN region r ON r.region_id=p.region_id WHERE r.region_code=?) AS related
                """, regionCode, regionCode, regionCode, regionCode, regionCode);
    }

    /** 중심 관광지 순위 상위 N + 장소 정보(좌표·주소·사진) + 연관 관광지 상위 3. */
    public List<Map<String, Object>> hubTop(String regionCode, int limit) {
        ensureHubTable();
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT h.base_ym, h.hub_rank, h.hub_name, h.hub_code, h.category_middle, p.place_id, p.source_provider,
                       p.address, p.latitude, p.longitude,
                       (SELECT i.image_url FROM place_image i WHERE i.place_id=p.place_id AND i.sort_order=0) AS image_url
                FROM place_hub_rank h JOIN region r ON r.region_id=h.region_id LEFT JOIN place p ON p.place_id=h.place_id
                WHERE r.region_code=? AND h.base_ym=(SELECT MAX(h2.base_ym) FROM place_hub_rank h2 WHERE h2.region_id=h.region_id)
                ORDER BY h.hub_rank LIMIT ?""", regionCode, limit);
        for (Map<String, Object> row : rows) {
            row.put("related", jdbc.queryForList("""
                    SELECT related_name, related_sigungu_name AS related_signgu_name, related_rank FROM related_attraction_rank
                    WHERE base_ym=? AND (attraction_code=? OR attraction_name=? OR base_place_id=?) ORDER BY related_rank LIMIT 3""",
                    row.get("base_ym"), row.get("hub_code"), row.get("hub_name"), row.get("place_id")));
        }
        return rows;
    }

    // ---- 저장 도우미 ----

    private long upsertPlace(long regionId, String provider, String sourceId, String name, String address, String detail,
                             BigDecimal lat, BigDecimal lng, String phone, LocalDateTime modified) {
        jdbc.update("""
                INSERT INTO place(region_id, source_provider, source_place_id, place_type, name, address, address_detail,
                  latitude, longitude, phone, source_modified_at, last_synced_at, is_active, created_at, updated_at, verification_status)
                VALUES (?,?,?,'ATTRACTION',?,?,?,?,?,?,?,NOW(),TRUE,NOW(),NOW(),'UNVERIFIED')
                ON DUPLICATE KEY UPDATE name=VALUES(name), address=COALESCE(VALUES(address), address),
                  address_detail=COALESCE(VALUES(address_detail), address_detail), latitude=COALESCE(VALUES(latitude), latitude),
                  longitude=COALESCE(VALUES(longitude), longitude), phone=COALESCE(VALUES(phone), phone),
                  source_modified_at=COALESCE(VALUES(source_modified_at), source_modified_at), is_active=TRUE, last_synced_at=NOW(), updated_at=NOW()""",
                regionId, provider, sourceId, name, blankToNull(address), blankToNull(detail), lat, lng, blankToNull(phone), modified);
        return jdbc.queryForObject("SELECT place_id FROM place WHERE source_provider=? AND source_place_id=?", Long.class, provider, sourceId);
    }

    private void upsertImage(long placeId, String url, String type, int order) {
        if (url == null || url.isBlank()) return;
        jdbc.update("""
                INSERT INTO place_image(place_id, image_url, image_type, sort_order, source_provider, created_at) VALUES (?,?,?,?,?,NOW())
                ON DUPLICATE KEY UPDATE image_url=VALUES(image_url), image_type=VALUES(image_type)""", placeId, url, type, order, P_TOUR);
    }

    private Map<String, Long> placeNames(long regionId) {
        Map<String, Long> map = new HashMap<>();
        // TourAPI 장소를 먼저 넣어 같은 이름이면 TourAPI 쪽(주소·사진 있음)을 쓴다.
        jdbc.query("SELECT place_id, name, source_provider FROM place WHERE region_id=? AND is_active=TRUE ORDER BY source_provider='KTO_TOURAPI'",
                rs -> { map.put(norm(rs.getString("name")), rs.getLong("place_id")); }, regionId);
        return map;
    }

    private StepResult log(long regionId, String provider, String type, String status, int total, int ok, String message) {
        return log(regionId, provider, type, status, total, ok, message, LocalDateTime.now());
    }

    private StepResult log(long regionId, String provider, String type, String status, int total, int ok, String message, LocalDateTime started) {
        jdbc.update("""
                INSERT INTO data_sync_log(region_id, provider, data_type, status, started_at, finished_at, total_count, success_count, failure_count, error_message)
                VALUES (?,?,?,?,?,NOW(),?,?,?,?)""", regionId, provider, type, status, started, total, ok, Math.max(0, total - ok), message);
        return new StepResult(provider, type, status, total, ok, message);
    }

    /** 이름 비교용: 괄호 안 내용, 「/경주」 꼬리, 앞의 지역 이름(경주), 공백·가운뎃점 제거. 예) 「경주 불국사 [유네스코 세계유산]」 = 「불국사」 */
    String norm(String name) {
        if (name == null) return "";
        String n = name.replaceAll("\\(.*?\\)|\\[.*?\\]", "").replaceAll("/.*$", "").replaceAll("[\\s·ㆍ.]", "").toLowerCase();
        if (!regionShort.isEmpty() && n.startsWith(regionShort) && n.length() > regionShort.length() + 1) n = n.substring(regionShort.length());
        return n;
    }

    private static String text(JsonNode node, String field) {
        JsonNode v = node.path(field);
        return v.isMissingNode() || v.isNull() ? null : v.asString("").trim();
    }

    private static BigDecimal decimal(JsonNode node, String field) {
        String v = text(node, field);
        try { return v == null || v.isEmpty() ? null : new BigDecimal(v).setScale(7, java.math.RoundingMode.HALF_UP); }
        catch (NumberFormatException e) { return null; }
    }

    private static int intOf(JsonNode node, String field) {
        String v = text(node, field);
        try { return v == null ? 0 : Integer.parseInt(v); } catch (NumberFormatException e) { return 0; }
    }

    private static LocalDateTime modified(String v) {
        try { return v == null || v.length() != 14 ? null : LocalDateTime.parse(v, TS); } catch (RuntimeException e) { return null; }
    }

    private static String blankToNull(String v) { return v == null || v.isBlank() ? null : v; }

    private static List<String> nonEmpty(String... values) {
        List<String> out = new ArrayList<>();
        for (String v : values) if (v != null && !v.isBlank()) out.add(v);
        return out;
    }
}
