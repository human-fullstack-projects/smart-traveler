package com.smarttraveler.tour.service;

import com.smarttraveler.tour.client.PlaceIndexClient;
import com.smarttraveler.tour.client.TourApiClient;
import com.smarttraveler.tour.client.TourApiClient.Page;
import com.smarttraveler.tour.client.TourApiClient.TourApiException;
import java.math.BigDecimal;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
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

    /** contentTypeId → place_type (12 관광지, 14 문화시설, 28 레포츠, 38 쇼핑, 39 음식점). */
    private static final Map<String, String> CONTENT_TYPE_TO_PLACE_TYPE = new LinkedHashMap<>();
    /** 파일럿 호출 한도를 아끼기 위한 타입별 상한. */
    private static final Map<String, Integer> PILOT_CAP = Map.of("12", 100, "14", 50, "28", 50, "38", 50, "39", 150);
    static {
        CONTENT_TYPE_TO_PLACE_TYPE.put("12", "ATTRACTION");
        CONTENT_TYPE_TO_PLACE_TYPE.put("14", "CULTURE");
        CONTENT_TYPE_TO_PLACE_TYPE.put("28", "LEISURE");
        CONTENT_TYPE_TO_PLACE_TYPE.put("38", "SHOPPING");
        CONTENT_TYPE_TO_PLACE_TYPE.put("39", "RESTAURANT");
    }

    /** KorService2 신분류(lclsSystm) 코드 이름표 · Phase 0에서 lclsSystmCode2로 확인(SH06=시장). */
    private static final Map<String, String> CATEGORY_NAMES = Map.ofEntries(
            Map.entry("AC", "숙박"), Map.entry("C01", "추천코스"), Map.entry("EV", "축제/공연/행사"), Map.entry("EX", "체험관광"),
            Map.entry("FD", "음식"), Map.entry("HS", "역사관광"), Map.entry("LS", "레저스포츠"), Map.entry("NA", "자연관광"),
            Map.entry("SH", "쇼핑"), Map.entry("VE", "문화관광"),
            Map.entry("AC01", "호텔"), Map.entry("AC02", "콘도미니엄"), Map.entry("AC03", "펜션/민박"), Map.entry("AC04", "모텔"),
            Map.entry("AC05", "캠핑"), Map.entry("AC06", "호스텔"),
            Map.entry("EV01", "축제"), Map.entry("EV02", "공연"), Map.entry("EV03", "행사"),
            Map.entry("FD01", "한식"), Map.entry("FD02", "외국식"), Map.entry("FD03", "간이음식"), Map.entry("FD04", "주점"), Map.entry("FD05", "카페/찻집"),
            Map.entry("HS01", "역사유적지"), Map.entry("HS02", "역사유물"), Map.entry("HS03", "종교성지"), Map.entry("HS04", "안보관광지"),
            Map.entry("LS01", "육상레저스포츠"), Map.entry("LS02", "수상레저스포츠"), Map.entry("LS03", "항공레저스포츠"), Map.entry("LS04", "복합레저스포츠"),
            Map.entry("NA01", "자연경관(산)"), Map.entry("NA02", "자연경관(하천·해양)"), Map.entry("NA03", "자연생태"), Map.entry("NA04", "자연공원"), Map.entry("NA05", "기타자연관광"),
            Map.entry("SH01", "백화점"), Map.entry("SH02", "쇼핑몰"), Map.entry("SH03", "대형마트"), Map.entry("SH04", "면세점"),
            Map.entry("SH05", "전문매장/상가"), Map.entry("SH06", "시장"), Map.entry("SH07", "기타쇼핑시설"),
            Map.entry("VE01", "랜드마크관광"), Map.entry("VE02", "테마공원"), Map.entry("VE03", "도시공원"), Map.entry("VE04", "도시·지역문화관광"),
            Map.entry("VE05", "복합관광시설"), Map.entry("VE06", "공연시설"), Map.entry("VE07", "전시시설"), Map.entry("VE08", "행사시설"),
            Map.entry("VE09", "교육시설"), Map.entry("VE10", "레저스포츠시설"), Map.entry("VE11", "교통시설"), Map.entry("VE12", "기타문화관광지"),
            Map.entry("EX01", "전통체험"), Map.entry("EX02", "공예체험"), Map.entry("EX03", "농.산.어촌 체험"), Map.entry("EX04", "산사체험"),
            Map.entry("EX05", "웰니스관광"), Map.entry("EX06", "산업관광"), Map.entry("EX07", "기타체험"),
            Map.entry("C0112", "가족코스"), Map.entry("C0113", "나홀로코스"), Map.entry("C0114", "힐링코스"),
            Map.entry("C0115", "도보코스"), Map.entry("C0116", "캠핑코스"), Map.entry("C0117", "맛코스"));

    private final Map<String, Long> categoryIdCache = new HashMap<>();

    private final TourApiClient api;
    private final JdbcTemplate jdbc;
    private final PlaceIndexClient indexClient;
    /** 이름 비교 때 떼어 낼 지역 이름 (예: 경주). sync 시작 때 정한다. */
    private volatile String regionShort = "";

    public TourSyncService(TourApiClient api, JdbcTemplate jdbc, PlaceIndexClient indexClient) {
        this.api = api;
        this.jdbc = jdbc;
        this.indexClient = indexClient;
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

        for (Map.Entry<String, String> contentType : CONTENT_TYPE_TO_PLACE_TYPE.entrySet()) {
            steps.add(syncPlaces(regionId, areaCd, signguTail, contentType.getKey(), contentType.getValue()));
        }
        String baseYm = latestBaseYm(areaCd, regionCode);
        if (baseYm == null) {
            steps.add(log(regionId, P_HUB, "HUB_RANK", "FAILED", 0, 0, "최근 6개월 기준 연월 자료 없음 또는 호출 실패 · 미반영"));
            steps.add(log(regionId, P_RELATED, "RELATED", "SKIPPED", 0, 0, "기준 연월을 찾지 못해 건너뜀 · 미반영"));
        } else {
            steps.add(syncHub(regionId, baseYm, areaCd, regionCode));
            steps.add(syncRelated(regionId, baseYm, areaCd, regionCode));
        }
        // ai-server의 /internal/places/index가 임시 롤백되어 있어 호출을 끔 (복구되면 아래 줄을 되살린다).
        // steps.add(indexPlaces(regionId));
        return new SyncResult((String) region.get("full_name"), baseYm, steps);
    }

    /** 활성·좌표 보유 장소를 100개씩 ai-server(/internal/places/index)로 보내 임베딩시킨다. */
    private StepResult indexPlaces(long regionId) {
        LocalDateTime started = LocalDateTime.now();
        List<Map<String, Object>> rows = placesForIndexing(regionId);
        int indexed = 0;
        boolean allOk = true;
        for (int i = 0; i < rows.size(); i += 100) {
            List<Map<String, Object>> batch = rows.subList(i, Math.min(i + 100, rows.size()));
            if (indexClient.index(batch)) indexed += batch.size(); else allOk = false;
        }
        return log(regionId, "AI_SERVER", "INDEX", allOk ? "SUCCESS" : "FAILED", rows.size(), indexed,
                allOk ? null : "임베딩 서버 연결 실패 · 다음 동기화에서 다시 시도", started);
    }

    /** ai-server PlaceRow 형식으로 변환한 활성·좌표 보유 장소 목록. */
    private List<Map<String, Object>> placesForIndexing(long regionId) {
        return jdbc.query("""
                SELECT p.place_id, p.place_type, p.name, p.address, p.description, p.closed_days, p.is_market,
                       r.region_code, p.latitude, p.longitude, rd.main_menu,
                       (SELECT i.image_url FROM place_image i WHERE i.place_id=p.place_id AND i.sort_order=0 ORDER BY i.place_image_id LIMIT 1) AS image_url,
                       (SELECT MIN(h.hub_rank) FROM place_hub_rank h WHERE h.place_id=p.place_id) AS hub_rank
                FROM place p JOIN region r ON r.region_id=p.region_id
                LEFT JOIN restaurant_detail rd ON rd.place_id=p.place_id
                WHERE p.region_id=? AND p.is_active=TRUE AND p.latitude IS NOT NULL AND p.longitude IS NOT NULL""",
                (rs, row) -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("placeId", rs.getLong("place_id"));
                    m.put("placeType", rs.getString("place_type"));
                    m.put("name", rs.getString("name"));
                    m.put("address", rs.getString("address"));
                    m.put("description", rs.getString("description"));
                    m.put("mainMenu", rs.getString("main_menu"));
                    m.put("closedDays", rs.getString("closed_days"));
                    m.put("isMarket", rs.getBoolean("is_market"));
                    m.put("sigunguCode", rs.getString("region_code"));
                    m.put("lat", rs.getBigDecimal("latitude").doubleValue());
                    m.put("lng", rs.getBigDecimal("longitude").doubleValue());
                    m.put("imageUrl", rs.getString("image_url"));
                    int hubRank = rs.getInt("hub_rank");
                    m.put("hubRank", rs.wasNull() ? null : hubRank);
                    return m;
                }, regionId);
    }

    /** TourAPI 장소(관광지·문화시설·레포츠·쇼핑·음식점) → place / place_image / place_category(_mapping) / restaurant_detail.
     * 상세(overview·이용시간 등)는 설명이 없거나 원본이 바뀐 장소만 다시 부른다(5-2 (4) 증분 수집). */
    StepResult syncPlaces(long regionId, String areaCd, String signguTail, String contentTypeId, String placeType) {
        LocalDateTime started = LocalDateTime.now();
        int total = 0, ok = 0;
        int cap = PILOT_CAP.getOrDefault(contentTypeId, 100);
        try {
            for (int pageNo = 1; pageNo <= 10 && ok < cap; pageNo++) {
                Page page = api.areaPage(areaCd, signguTail, contentTypeId, pageNo, 100);
                total = page.totalCount();
                for (JsonNode it : page.items()) {
                    if (ok >= cap) break;
                    String contentId = text(it, "contentid");
                    String name = text(it, "title");
                    String topCode = text(it, "lclsSystm1");
                    String subCode = text(it, "lclsSystm2");
                    LocalDateTime modified = modified(text(it, "modifiedtime"));
                    boolean market = isMarket(placeType, subCode, name);
                    long placeId = upsertPlace(regionId, P_TOUR, contentId, name, placeType,
                            text(it, "addr1"), text(it, "addr2"), decimal(it, "mapy"), decimal(it, "mapx"),
                            text(it, "tel"), modified, recommendedMinutes(placeType), market);
                    upsertImage(placeId, text(it, "firstimage"), "MAIN", 0);
                    upsertImage(placeId, text(it, "firstimage2"), "THUMB", 1);
                    linkCategory(placeId, topCode, subCode);
                    if (needsDetail(placeId, modified)) fetchDetail(placeId, contentId, contentTypeId, placeType);
                    ok++;
                }
                if (pageNo * 100 >= total || page.items().isEmpty()) break;
            }
            return log(regionId, P_TOUR, placeType, "SUCCESS", total, ok, null, started);
        } catch (TourApiException e) {
            return log(regionId, P_TOUR, placeType, "FAILED", total, ok, e.getMessage() + " · 기존 데이터 유지(미반영)", started);
        }
    }

    /** 설명이 없거나 원본이 마지막 동기화 뒤에 바뀐 장소만 상세를 다시 부른다. */
    private boolean needsDetail(long placeId, LocalDateTime modified) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT description, source_modified_at FROM place WHERE place_id=?", placeId);
        if (rows.isEmpty()) return true;
        Object description = rows.get(0).get("description");
        Object lastModified = rows.get(0).get("source_modified_at");
        if (description == null) return true;
        return modified != null && lastModified instanceof LocalDateTime lm && modified.isAfter(lm);
    }

    /** detailCommon2(overview·홈페이지) + detailIntro2(이용시간·휴무·대표메뉴) 수집. 실패해도 기존 값은 유지한다. */
    private void fetchDetail(long placeId, String contentId, String contentTypeId, String placeType) {
        try {
            Page common = api.detailCommon(contentId);
            if (!common.items().isEmpty()) {
                JsonNode c = common.items().get(0);
                String overview = blankToNull(stripHtml(text(c, "overview")));
                String homepage = blankToNull(extractUrl(text(c, "homepage")));
                jdbc.update("UPDATE place SET description=COALESCE(?,description), homepage_url=COALESCE(?,homepage_url) WHERE place_id=?",
                        overview, homepage, placeId);
            }
        } catch (TourApiException ignored) {
            // 기존 설명 유지(미반영)
        }
        try {
            Page intro = api.detailIntro(contentId, contentTypeId);
            if (!intro.items().isEmpty()) {
                JsonNode n = intro.items().get(0);
                String useTime = blankToNull(firstNonBlank(text(n, "usetime"), text(n, "usetimeculture"),
                        text(n, "usetimeleports"), text(n, "opentime"), text(n, "opentimefood")));
                String restDate = blankToNull(firstNonBlank(text(n, "restdate"), text(n, "restdateculture"),
                        text(n, "restdateleports"), text(n, "restdateshopping"), text(n, "restdatefood")));
                jdbc.update("UPDATE place SET opening_hours=COALESCE(?,opening_hours), closed_days=COALESCE(?,closed_days) WHERE place_id=?",
                        useTime, restDate, placeId);
                if ("RESTAURANT".equals(placeType)) {
                    String menu = blankToNull(firstNonBlank(text(n, "firstmenu"), text(n, "treatmenu")));
                    if (menu != null) jdbc.update("""
                            INSERT INTO restaurant_detail(place_id, main_menu, created_at, updated_at) VALUES (?,?,NOW(),NOW())
                            ON DUPLICATE KEY UPDATE main_menu=VALUES(main_menu), updated_at=NOW()""", placeId, menu);
                }
            }
        } catch (TourApiException ignored) {
            // 기존 이용시간 유지(미반영)
        }
    }

    /** 쇼핑(SH06=시장) 분류이거나, 관광지로 등록된 시장(예: 자갈치시장)을 이름으로 판정한다. */
    private static boolean isMarket(String placeType, String subCode, String name) {
        boolean byCategory = "SH06".equals(subCode);
        boolean byName = name != null && name.contains("시장")
                && !name.contains("마트") && !name.contains("백화점") && !name.contains("면세");
        if ("SHOPPING".equals(placeType)) return byCategory || byName;
        if ("ATTRACTION".equals(placeType)) return byName;
        return false;
    }

    /** 4-1 슬롯 표의 기본 체류시간(분). */
    private static int recommendedMinutes(String placeType) {
        return switch (placeType) {
            case "RESTAURANT", "SHOPPING" -> 60;
            default -> 90; // ATTRACTION, CULTURE, LEISURE
        };
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) if (v != null && !v.isBlank()) return v;
        return null;
    }

    private static String stripHtml(String v) {
        if (v == null) return null;
        return v.replaceAll("(?i)<br\\s*/?>", "\n").replaceAll("<[^>]+>", "").trim();
    }

    private static final Pattern URL_PATTERN = Pattern.compile("https?://[^\"'>\\s]+");

    private static String extractUrl(String v) {
        if (v == null) return null;
        var m = URL_PATTERN.matcher(v);
        return m.find() ? m.group() : null;
    }

    /** lclsSystm1(대분류)·lclsSystm2(소분류)를 place_category(_mapping)에 연결한다. */
    private void linkCategory(long placeId, String topCode, String subCode) {
        if (topCode == null || topCode.isBlank()) return;
        long topId = upsertCategory(topCode, CATEGORY_NAMES.getOrDefault(topCode, topCode), 1, topCode, topCode, null);
        jdbc.update("INSERT IGNORE INTO place_category_mapping(category_id, place_id) VALUES (?,?)", topId, placeId);
        if (subCode != null && !subCode.isBlank()) {
            long subId = upsertCategory(subCode, CATEGORY_NAMES.getOrDefault(subCode, subCode), 2, topCode, topCode + ">" + subCode, topId);
            jdbc.update("INSERT IGNORE INTO place_category_mapping(category_id, place_id) VALUES (?,?)", subId, placeId);
        }
    }

    private long upsertCategory(String code, String name, int depth, String groupCode, String path, Long parentId) {
        Long cached = categoryIdCache.get(code);
        if (cached != null) return cached;
        List<Long> found = jdbc.query("SELECT category_id FROM place_category WHERE category_code=?", (rs, row) -> rs.getLong(1), code);
        long id;
        if (!found.isEmpty()) {
            id = found.get(0);
        } else {
            var key = new GeneratedKeyHolder();
            jdbc.update(connection -> {
                var st = connection.prepareStatement(
                        "INSERT INTO place_category(category_code, category_name, category_path, depth, group_code, is_active, parent_category_id) VALUES (?,?,?,?,?,TRUE,?)",
                        Statement.RETURN_GENERATED_KEYS);
                st.setString(1, code);
                st.setString(2, name);
                st.setString(3, path);
                st.setInt(4, depth);
                st.setString(5, groupCode);
                if (parentId == null) st.setNull(6, Types.BIGINT); else st.setLong(6, parentId);
                return st;
            }, key);
            id = Objects.requireNonNull(key.getKey()).longValue();
        }
        categoryIdCache.put(code, id);
        return id;
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
                        placeId = upsertPlace(regionId, P_HUB, text(it, "hubTatsCd"), name, "ATTRACTION", null, null,
                                decimal(it, "mapY"), decimal(it, "mapX"), null, null, recommendedMinutes("ATTRACTION"), false);
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

    private long upsertPlace(long regionId, String provider, String sourceId, String name, String placeType, String address, String detail,
                             BigDecimal lat, BigDecimal lng, String phone, LocalDateTime modified, int recommendedMinutes, boolean isMarket) {
        jdbc.update("""
                INSERT INTO place(region_id, source_provider, source_place_id, place_type, name, address, address_detail,
                  latitude, longitude, phone, recommended_minutes, is_market, source_modified_at, last_synced_at, is_active, created_at, updated_at, verification_status)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,NOW(),TRUE,NOW(),NOW(),'UNVERIFIED')
                ON DUPLICATE KEY UPDATE name=VALUES(name), place_type=VALUES(place_type), address=COALESCE(VALUES(address), address),
                  address_detail=COALESCE(VALUES(address_detail), address_detail), latitude=COALESCE(VALUES(latitude), latitude),
                  longitude=COALESCE(VALUES(longitude), longitude), phone=COALESCE(VALUES(phone), phone),
                  recommended_minutes=COALESCE(recommended_minutes, VALUES(recommended_minutes)), is_market=VALUES(is_market),
                  source_modified_at=COALESCE(VALUES(source_modified_at), source_modified_at), is_active=TRUE, last_synced_at=NOW(), updated_at=NOW()""",
                regionId, provider, sourceId, placeType, name, blankToNull(address), blankToNull(detail), lat, lng, blankToNull(phone),
                recommendedMinutes, isMarket, modified);
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
