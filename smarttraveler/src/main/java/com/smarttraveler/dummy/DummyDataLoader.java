package com.smarttraveler.dummy;

import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** Classpath fixtures are shared by all teammates; every insert is repeatable and local. */
@Component
public class DummyDataLoader implements ApplicationRunner {
    private static final Logger LOG = LoggerFactory.getLogger(DummyDataLoader.class);
    private static final String PASSWORD = "123456";
    private static final Map<String, OwnerName> OWNER_NAMES = readOwnerNames();
    private static final String ROOM_NOTE = "[예시 객실·가격] 실제 예약 상품이 아닙니다";
    private static final Pattern TIME = Pattern.compile("(?:^|[^0-9])([01]?[0-9]|2[0-3])[:시.]?([0-5][0-9])?(?:[^0-9]|$)");
    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwords;
    private final TransactionTemplate transactions;
    private final ObjectMapper json = new ObjectMapper();
    private final boolean enabled;
    private final String datasourceUrl;

    public DummyDataLoader(JdbcTemplate jdbc, PasswordEncoder passwords, PlatformTransactionManager manager,
                           @Value("${app.dummy-data.enabled:true}") boolean enabled,
                           @Value("${spring.datasource.url}") String datasourceUrl) {
        this.jdbc = jdbc;
        this.passwords = passwords;
        this.transactions = new TransactionTemplate(manager);
        this.enabled = enabled;
        this.datasourceUrl = datasourceUrl;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) return;
        if (!isLocalDatasource(datasourceUrl)) {
            LOG.info("더미 데이터 적재 건너뜀: 로컬 데이터베이스가 아님");
            return;
        }
        JsonNode lodgings = resource("lodgings.json");
        JsonNode places = resource("places.json");
        if (lodgings == null || places == null) return;
        try {
            jdbc.update("""
                    INSERT INTO owner_review_status(review_status_code,review_status_name)
                    SELECT 'APPROVED','승인' FROM DUAL
                    WHERE NOT EXISTS (SELECT 1 FROM owner_review_status WHERE review_status_code='APPROVED')
                    """);
        } catch (RuntimeException e) {
            LOG.warn("업주 승인 코드 준비 실패: {}", e.getMessage());
        }
        String hash = passwords.encode(PASSWORD);
        int ownerCount = 0;
        int roomCount = 0;
        int placeCount = 0;
        int userCount = 0;
        for (int i = 1; i <= 5; i++) {
            final int number = i;
            try {
                Integer added = transactions.execute(status -> {
                    if (!migrateUser(number, hash)) return 0;
                    return member("user" + number, "USER", "여행자" + number, hash) ? 1 : 0;
                });
                userCount += added == null ? 0 : added;
            } catch (RuntimeException e) {
                LOG.warn("더미 여행자 {} 적재 실패: {}", i, e.getMessage());
            }
        }
        int number = 0;
        for (JsonNode row : lodgings) {
            number++;
            final int index = number;
            try {
                int[] added = transactions.execute(status -> lodging(row, index, hash));
                if (added != null) {
                    ownerCount += added[0];
                    roomCount += added[1];
                }
            } catch (RuntimeException e) {
                LOG.warn("더미 숙소 {} 적재 실패: {}", index, e.getMessage());
            }
        }
        for (JsonNode row : places) {
            try {
                Integer added = transactions.execute(status -> place(row));
                placeCount += added == null ? 0 : added;
            } catch (RuntimeException e) {
                LOG.warn("더미 관광지 {} 적재 실패: {}", text(row, "source_content_id"), e.getMessage());
            }
        }
        LOG.info("더미 데이터 새로 추가: 업주/숙소 {}, 객실 {}, 여행자 {}, 관광지 {}", ownerCount, roomCount, userCount, placeCount);
    }

    static boolean isLocalDatasource(String url) {
        if (url == null || !url.startsWith("jdbc:")) return false;
        try {
            String host = URI.create(url.substring(5)).getHost();
            return "localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host) || "[::1]".equals(host);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private JsonNode resource(String name) {
        try (InputStream stream = getClass().getResourceAsStream("/dummy/" + name)) {
            if (stream == null) {
                LOG.warn("더미 데이터 파일 없음: {}", name);
                return null;
            }
            JsonNode node = json.readTree(stream);
            if (!node.isArray()) throw new IllegalArgumentException("배열이 아닙니다");
            return node;
        } catch (Exception e) {
            LOG.warn("더미 데이터 파일 {} 읽기 실패: {}", name, e.getMessage());
            return null;
        }
    }

    private boolean member(String login, String role, String nickname, String hash) {
        return jdbc.update("""
                INSERT INTO members(login_id,password,nickname,email,status,role_code,created_at,updated_at)
                SELECT ?,?,?,?,'ACTIVE',?,NOW(),NOW() FROM DUAL
                WHERE NOT EXISTS (SELECT 1 FROM members WHERE login_id=?)
                """, login, hash, nickname, login + "@example.test", role, login) == 1;
    }

    record OwnerName(int index, String name) {}

    static Map<String, OwnerName> ownerNames() { return OWNER_NAMES; }

    private static Map<String, OwnerName> readOwnerNames() {
        Map<String, OwnerName> result = new LinkedHashMap<>();
        try (InputStream stream = DummyDataLoader.class.getResourceAsStream("/dummy/owner_names.csv")) {
            if (stream == null) throw new IllegalStateException("owner_names.csv 없음");
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
            if (!"source_content_id,index,name".equals(reader.readLine()))
                throw new IllegalStateException("owner_names.csv 헤더 오류");
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",", -1);
                if (parts.length != 3 || result.put(parts[0], new OwnerName(Integer.parseInt(parts[1]), parts[2])) != null)
                    throw new IllegalStateException("owner_names.csv 중복/형식 오류: " + line);
            }
            if (result.size() != 145) throw new IllegalStateException("가상 업주 명단은 145명이어야 함");
            return Map.copyOf(result);
        } catch (Exception e) {
            throw new IllegalStateException("가상 업주 명단 읽기 실패", e);
        }
    }

    boolean migrateUser(int number, String hash) {
        String old = "dummy_user_" + number;
        String target = "user" + number;
        Long oldId = id("SELECT member_id FROM members WHERE login_id=? AND email=? AND role_code='USER'",
                old, old + "@example.test");
        if (oldId == null) return true;
        if (id("SELECT member_id FROM members WHERE login_id=? OR email=?", target, target + "@example.test") != null) {
            LOG.warn("더미 여행자 {} 이행 건너뜀: 목표 계정 사용 중", number);
            return false;
        }
        jdbc.update("UPDATE members SET login_id=?,email=?,password=?,nickname=CASE WHEN nickname=? THEN ? ELSE nickname END,updated_at=NOW() WHERE member_id=? AND login_id=? AND email=? AND role_code='USER'",
                target, target + "@example.test", hash, "여행자 예시 " + number, "여행자" + number,
                oldId, old, old + "@example.test");
        return true;
    }

    private boolean migrateOwner(String sourceId, OwnerName owner, String hash) {
        String old = "dummy_owner_" + sourceId;
        String target = "owner" + owner.index();
        String business = "DUMMY-" + sourceId;
        Long oldId = id("""
                SELECT m.member_id FROM members m JOIN owner_profiles o ON o.member_id=m.member_id
                WHERE m.login_id=? AND m.email=? AND m.role_code='OWNER' AND o.business_number=?
                """, old, old + "@example.test", business);
        if (oldId == null) return true;
        if (id("SELECT member_id FROM members WHERE login_id=? OR email=?", target, target + "@example.test") != null) {
            LOG.warn("더미 업주 {} 이행 건너뜀: 목표 계정 사용 중", sourceId);
            return false;
        }
        jdbc.update("""
                UPDATE members SET login_id=?,email=?,password=?,
                    nickname=CASE WHEN nickname LIKE '숙소 예시 %' THEN ? ELSE nickname END,updated_at=NOW()
                WHERE member_id=? AND login_id=? AND email=? AND role_code='OWNER'
                """, target, target + "@example.test", hash, owner.name(), oldId, old, old + "@example.test");
        jdbc.update("""
                UPDATE owner_profiles SET representative_name=?,updated_at=NOW()
                WHERE member_id=? AND business_number=? AND representative_name='예시 업주'
                """, owner.name(), oldId, business);
        return true;
    }

    int[] lodging(JsonNode row, int index, String hash) {
        String sourceId = text(row, "source_content_id");
        if (sourceId == null || sourceId.isBlank()) return new int[]{0, 0};
        OwnerName owner = OWNER_NAMES.get(sourceId);
        if (owner == null) throw new IllegalArgumentException("업주 이름 매핑 없음: " + sourceId);
        index = owner.index();
        if (!migrateOwner(sourceId, owner, hash)) return new int[]{0, 0};
        String login = "owner" + index;
        String business = "DUMMY-" + sourceId;
        Long existingId = id("SELECT lodging_id FROM lodging WHERE source_provider='TOURAPI' AND source_content_id=?", sourceId);
        if (existingId != null && id("""
                SELECT l.lodging_id FROM lodging l
                JOIN owner_profiles o ON o.owner_id=l.owner_id
                JOIN members m ON m.member_id=o.member_id
                WHERE l.lodging_id=? AND l.source_provider='TOURAPI' AND m.login_id=?
                  AND m.email=? AND m.role_code='OWNER' AND o.business_number=?
                """, existingId, login, login + "@example.test", business) == null) {
            LOG.warn("더미 숙소 {} 건너뜀: 기존 숙소 소유자가 더미 업주와 일치하지 않음", sourceId);
            return new int[]{0, 0};
        }
        boolean newMember = member(login, "OWNER", owner.name(), hash);
        Long memberId = id("SELECT member_id FROM members WHERE login_id=?", login);
        if (memberId == null) return new int[]{0, 0};
        if (!newMember && dummyOwnerId(memberId, login, business) == null) {
            LOG.warn("더미 숙소 {} 건너뜀: 기존 계정이 더미 업주 프로필과 일치하지 않음", sourceId);
            return new int[]{0, 0};
        }
        String area = text(row, "area_code");
        Long regionId = region(row);
        String name = text(row, "name");
        String address = text(row, "address");
        Double lat = decimal(row, "latitude");
        Double lng = decimal(row, "longitude");
        if (regionId == null || name == null || address == null || lat == null || lng == null) {
            throw new IllegalArgumentException("필수 숙소 정보 부족: " + sourceId + ", 지역 " + area);
        }
        jdbc.update("""
                INSERT INTO owner_profiles(member_id,business_number,business_name,representative_name,
                    business_address,business_phone,review_status_code,created_at,updated_at)
                SELECT ?,?,?,?,? ,?,'APPROVED',NOW(),NOW() FROM DUAL
                WHERE NOT EXISTS (SELECT 1 FROM owner_profiles WHERE member_id=?)
                """, memberId, business, limit(name, 100), owner.name(), limit(address, 255),
                String.format("000-0000-%04d", index), memberId);
        Long ownerId = dummyOwnerId(memberId, login, business);
        if (ownerId == null) return new int[]{0, 0};
        String type = text(row, "lodging_type");
        if (!List.of("HOTEL", "PENSION", "HANOK").contains(type)) type = "PENSION";
        int addedLodging = jdbc.update("""
                INSERT INTO lodging(owner_id,region_id,source_provider,source_content_id,lodging_type,
                    name,address,address_detail,zipcode,latitude,longitude,phone,homepage_url,description,
                    checkin_time,checkout_time,parking_info,status,approved_at,last_synced_at,created_at,updated_at)
                SELECT ?,?,'TOURAPI',?,?,?,?,?,?,?,?,?,?,?,?,?,?,'ACTIVE',NOW(),NOW(),NOW(),NOW() FROM DUAL
                WHERE NOT EXISTS (SELECT 1 FROM lodging WHERE source_provider='TOURAPI' AND source_content_id=?)
                """, ownerId, regionId, sourceId, type, limit(name, 200), limit(address, 300),
                limit(text(row, "address_detail"), 200), limit(text(row, "zipcode"), 10), lat, lng,
                limit(text(row, "phone"), 30), limit(text(row, "homepage_url"), 1000),
                "[예시 숙소 데이터] " + value(row, "description", "한국관광공사 TourAPI 제공 숙소"),
                time(text(row, "checkin_time"), "15:00:00"), time(text(row, "checkout_time"), "11:00:00"),
                limit(text(row, "parking_info"), 200), sourceId);
        if (existingId != null) {
            jdbc.update("""
                    UPDATE lodging SET region_id=?,lodging_type=?,name=?,address=?,address_detail=?,zipcode=?,
                        latitude=?,longitude=?,phone=?,homepage_url=?,description=?,checkin_time=?,
                        checkout_time=?,parking_info=?
                    WHERE lodging_id=? AND source_provider='TOURAPI' AND source_content_id=?
                      AND EXISTS (SELECT 1 FROM owner_profiles o JOIN members m ON m.member_id=o.member_id
                                  WHERE o.owner_id=lodging.owner_id AND m.login_id=?
                                    AND m.email=? AND m.role_code='OWNER'
                                     AND o.business_number=?)
                    """, regionId, type, limit(name, 200), limit(address, 300),
                    limit(text(row, "address_detail"), 200), limit(text(row, "zipcode"), 10), lat, lng,
                    limit(text(row, "phone"), 30), limit(text(row, "homepage_url"), 1000),
                    "[예시 숙소 데이터] " + value(row, "description", "한국관광공사 TourAPI 제공 숙소"),
                    time(text(row, "checkin_time"), "15:00:00"), time(text(row, "checkout_time"), "11:00:00"),
                    limit(text(row, "parking_info"), 200), existingId, sourceId, login,
                    login + "@example.test", business);
        }
        Long lodgingId = id("SELECT lodging_id FROM lodging WHERE source_provider='TOURAPI' AND source_content_id=?", sourceId);
        if (lodgingId == null) return new int[]{addedLodging, 0};
        photo(lodgingId, text(row, "image_url"), text(row, "thumbnail_url"));
        int rooms = 0;
        RoomCapacity.Spec[] specs = roomSpecs(row, type);
        List<Long> existingRooms = jdbc.queryForList("""
                SELECT room_id FROM room WHERE lodging_id=? AND description=? ORDER BY room_id LIMIT 3
                """, Long.class, lodgingId, ROOM_NOTE);
        int[] prices = "HOTEL".equals(type) ? new int[]{110000, 160000, 220000}
                : "HANOK".equals(type) ? new int[]{95000, 135000, 185000}
                : new int[]{80000, 115000, 165000};
        for (int i = 0; i < specs.length; i++) {
            RoomCapacity.Spec spec = specs[i];
            int price = prices[i];
            if (i < existingRooms.size()) {
                Long roomId = existingRooms.get(i);
                if (id("SELECT room_id FROM room WHERE lodging_id=? AND name=? AND room_id<>? LIMIT 1",
                        lodgingId, spec.name(), roomId) != null) {
                    LOG.warn("더미 객실 {} 이름 갱신 건너뜀: 같은 숙소에 '{}' 이름이 이미 있음", roomId, spec.name());
                    continue;
                }
                jdbc.update("""
                        UPDATE room SET name=?,base_capacity=?,max_capacity=?
                        WHERE room_id=? AND lodging_id=? AND description=?
                        """, spec.name(), spec.base(), spec.max(), roomId, lodgingId, ROOM_NOTE);
                continue;
            }
            rooms += jdbc.update("""
                    INSERT INTO room(lodging_id,name,base_capacity,max_capacity,size_m2,bed_type,
                        price_off_weekday,price_off_weekend,price_peak_weekday,price_peak_weekend,
                        amenities,description,is_active,created_at,updated_at)
                    SELECT ?,?,?,?,?,?,?,?,?,?,?,?,TRUE,NOW(),NOW() FROM DUAL
                    WHERE NOT EXISTS (SELECT 1 FROM room WHERE lodging_id=? AND name=?)
                    """, lodgingId, spec.name(), spec.base(), spec.max(), i == 0 ? 22 : i == 1 ? 32 : 48,
                    "PENSION".equals(type) ? "침구" : "침대", price, price + 20000,
                    price + 30000, price + 50000, "와이파이, 냉난방", ROOM_NOTE, lodgingId, spec.name());
        }
        return new int[]{addedLodging, rooms};
    }

    private Long dummyOwnerId(Long memberId, String login, String business) {
        return id("""
                SELECT o.owner_id FROM owner_profiles o
                JOIN members m ON m.member_id=o.member_id
                WHERE o.member_id=? AND m.login_id=? AND m.email=? AND m.role_code='OWNER'
                  AND o.business_number=?
                """, memberId, login, login + "@example.test", business);
    }

    private int place(JsonNode row) {
        String sourceId = text(row, "source_content_id");
        String name = text(row, "name");
        if (sourceId == null || name == null) return 0;
        Long regionId = region(row);
        if (regionId == null) throw new IllegalArgumentException("관광지 지역 없음: " + sourceId);
        int added = jdbc.update("""
                INSERT INTO place(region_id,source_provider,source_place_id,place_type,name,address,address_detail,
                    latitude,longitude,phone,last_synced_at,is_active,created_at,updated_at,verification_status)
                SELECT ?,'KTO_TOURAPI',?,'ATTRACTION',?,?,?,?,?,?,NOW(),TRUE,NOW(),NOW(),'UNVERIFIED' FROM DUAL
                WHERE NOT EXISTS (SELECT 1 FROM place WHERE source_provider='KTO_TOURAPI' AND source_place_id=?)
                """, regionId, sourceId, limit(name, 200), limit(text(row, "address"), 300),
                limit(text(row, "address_detail"), 200), decimal(row, "latitude"), decimal(row, "longitude"),
                limit(text(row, "phone"), 30), sourceId);
        Long placeId = id("SELECT place_id FROM place WHERE source_provider='KTO_TOURAPI' AND source_place_id=?", sourceId);
        if (placeId != null) {
            placeImage(placeId, text(row, "image_url"), "MAIN", 0);
            placeImage(placeId, text(row, "thumbnail_url"), "THUMB", 1);
        }
        return added;
    }

    private Long region(JsonNode row) {
        String code = text(row, "area_code");
        String name = text(row, "area_name");
        if (code == null || name == null) return null;
        jdbc.update("""
                INSERT INTO region(region_code,region_name,full_name,region_level,is_supported,created_at,updated_at)
                SELECT ?,?,?,'SIDO',TRUE,NOW(),NOW() FROM DUAL
                WHERE NOT EXISTS (SELECT 1 FROM region WHERE region_code=?)
                """, code, name, name, code);
        String signguCode = text(row, "region_code");
        String signguName = text(row, "region_name");
        if (signguCode != null && signguCode.matches("[0-9]{5}") && signguCode.startsWith(code)
                && signguName != null && !signguName.isBlank()) {
            jdbc.update("""
                    INSERT INTO region(parent_region_id,region_code,region_name,full_name,
                        region_level,is_supported,created_at,updated_at)
                    SELECT (SELECT region_id FROM region WHERE region_code=?),?,?,?,'SIGUNGU',TRUE,NOW(),NOW() FROM DUAL
                    WHERE NOT EXISTS (SELECT 1 FROM region WHERE region_code=?)
                    """, code, signguCode, signguName, name + " " + signguName, signguCode);
            return id("SELECT region_id FROM region WHERE region_code=?", signguCode);
        }
        String address = text(row, "address");
        if (address != null) {
            List<Map<String, Object>> matching = jdbc.queryForList("""
                    SELECT region_id,region_name FROM region WHERE region_level='SIGUNGU' AND region_code LIKE ?
                    ORDER BY LENGTH(region_name) DESC
                    """, code + "%");
            for (Map<String, Object> candidate : matching) {
                if (address.contains(String.valueOf(candidate.get("region_name"))))
                    return ((Number) candidate.get("region_id")).longValue();
            }
        }
        return id("SELECT region_id FROM region WHERE region_code=?", code);
    }

    private void photo(Long lodgingId, String image, String thumb) {
        if (image == null || image.isBlank()) return;
        jdbc.update("""
                INSERT INTO lodging_photo(lodging_id,image_url,thumbnail_url,source_provider,sort_order,created_at)
                SELECT ?,?,?,'TOURAPI',1,NOW() FROM DUAL
                WHERE NOT EXISTS (SELECT 1 FROM lodging_photo WHERE lodging_id=? AND image_url=?)
                """, lodgingId, limit(image, 1000), limit(thumb, 1000), lodgingId, limit(image, 1000));
    }

    private void placeImage(Long placeId, String image, String type, int order) {
        if (image == null || image.isBlank()) return;
        jdbc.update("""
                INSERT INTO place_image(place_id,image_url,image_type,sort_order,source_provider,created_at)
                SELECT ?,?,?,?,'KTO_TOURAPI',NOW() FROM DUAL
                WHERE NOT EXISTS (SELECT 1 FROM place_image WHERE place_id=? AND sort_order=?)
                """, placeId, limit(image, 1000), type, order, placeId, order);
    }

    private Long id(String sql, Object... parameters) {
        List<Long> found = jdbc.queryForList(sql, Long.class, parameters);
        return found.isEmpty() ? null : found.get(0);
    }

    private RoomCapacity.Spec[] roomSpecs(JsonNode row, String type) {
        String[] defaults = "HANOK".equals(type) ? new String[]{"온돌", "별채", "가족실"}
                : new String[]{"스탠다드", "디럭스", "패밀리"};
        JsonNode actual = row.path("rooms");
        JsonNode oldNames = row.path("room_names");
        RoomCapacity.Spec[] specs = new RoomCapacity.Spec[3];
        for (int i = 0; i < specs.length; i++) {
            String candidate = i < actual.size() ? actual.get(i).path("name").asString("").trim() : "";
            if (candidate.isBlank() && i < oldNames.size()) candidate = oldNames.get(i).asString("").trim();
            String name = candidate.isBlank() ? defaults[i] : limit(candidate, 80);
            specs[i] = RoomCapacity.from(i < actual.size() ? actual.get(i) : null,
                    name, i == 0 ? 2 : i == 1 ? 2 : 4, i == 0 ? 2 : i == 1 ? 4 : 6);
        }
        for (int i = 1; i < specs.length; i++) {
            boolean duplicate = false;
            for (int previous = 0; previous < i; previous++) duplicate |= specs[i].name().equals(specs[previous].name());
            if (duplicate) specs[i] = new RoomCapacity.Spec(limit(specs[i].name() + " " + (i + 1), 100),
                    specs[i].base(), specs[i].max());
        }
        return specs;
    }

    private String time(String raw, String fallback) {
        if (raw == null) return fallback;
        Matcher matcher = TIME.matcher(raw);
        if (!matcher.find()) return fallback;
        int hour = Integer.parseInt(matcher.group(1));
        int minute = matcher.group(2) == null ? 0 : Integer.parseInt(matcher.group(2));
        return LocalTime.of(hour, minute).toString();
    }

    private String text(JsonNode node, String key) {
        JsonNode value = node.path(key);
        return value.isMissingNode() || value.isNull() ? null : value.asString("").trim();
    }

    private Double decimal(JsonNode node, String key) {
        JsonNode value = node.path(key);
        return value.isMissingNode() || value.isNull() ? null : value.asDouble();
    }

    private String value(JsonNode node, String key, String fallback) {
        String result = text(node, key);
        return result == null || result.isBlank() ? fallback : result;
    }

    private String limit(String value, int max) {
        return value == null ? null : value.substring(0, Math.min(value.length(), max));
    }
}
