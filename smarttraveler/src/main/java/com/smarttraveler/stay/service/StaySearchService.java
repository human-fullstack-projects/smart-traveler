package com.smarttraveler.stay.service;

import com.smarttraveler.stay.dto.StaySearchDtos.Item;
import com.smarttraveler.stay.dto.StaySearchDtos.MapPoint;
import com.smarttraveler.stay.dto.StaySearchDtos.MapResult;
import com.smarttraveler.stay.dto.StaySearchDtos.Page;
import com.smarttraveler.stay.dto.StaySearchDtos.Region;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * 여행자 숙소 찾기: 조건(지역·종류·편의시설·이름)은 서버 SQL로 거르고, 페이지(5/10곳)만 돌려준다.
 * 빈방·최저가는 숙소마다 따로 묻지 않고 한 번의 SQL로 계산한다(room_inventory의 (room_id, stay_date) 색인 사용).
 * - 기본(가까운 순·이름순): 숙소를 먼저 고른 뒤 그 쪽 숙소들의 빈방만 계산 → 빠름
 * - 가격순 · 예약 가능만 · 최대 요금: 조건에 맞는 숙소 전체의 빈방을 한 번에 계산
 */
@Service
@RequiredArgsConstructor
public class StaySearchService {
    public static final Set<String> TYPES = Set.of("HOTEL", "MOTEL", "PENSION", "RESORT", "GUESTHOUSE", "HANOK", "CAMPING");
    private static final int MAP_POINT_LIMIT = 120;

    private final JdbcTemplate jdbc;
    private final StayInventoryService inventory;

    /** 검색 조건. sort = distance(기본) | price | name. 그 밖의 값(rating 등)은 기본 순서. */
    public record Query(Long sidoId, Long sigunguId, List<String> types, List<String> amenities, String keyword,
                        LocalDate checkIn, LocalDate checkOut, int guests, Integer maxPrice, boolean bookableOnly,
                        String sort, Double centerLat, Double centerLng) {}

    public Page search(Query q, int page, int size) {
        validate(q);
        if (page < 0 || size < 1 || size > 50) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "페이지 값을 확인해 주세요.");
        Filter f = filter(q, null);
        double[] center = center(q, f);
        boolean needsAll = "price".equals(q.sort()) || q.bookableOnly() || q.maxPrice() != null;
        List<Long> ids;
        long total;
        Map<Long, Avail> avail;
        double[] bounds;
        if (needsAll) {
            Map<Long, Avail> all = availability(q, f.where(), f.args());
            List<Long> matched = new ArrayList<>();
            for (var e : all.entrySet()) if (q.maxPrice() == null || e.getValue().price() <= q.maxPrice()) matched.add(e.getKey());
            Map<Long, double[]> coords = coords(matched);
            bounds = boundsOf(coords.values());
            if ("price".equals(q.sort())) matched.sort((a, b) -> Integer.compare(all.get(a).price(), all.get(b).price()) != 0
                ? Integer.compare(all.get(a).price(), all.get(b).price()) : Long.compare(a, b));
            else if ("name".equals(q.sort())) matched = orderedByName(matched);
            else matched.sort((a, b) -> Double.compare(dist(coords.get(a), center), dist(coords.get(b), center)));
            total = matched.size();
            long offset = (long) page * size;
            int from = (int) Math.min(offset, matched.size());
            int to = (int) Math.min(offset + size, matched.size());
            ids = matched.subList(from, to);
            avail = all;
        } else {
            var summary = jdbc.queryForMap("SELECT COUNT(*) n, MIN(l.latitude) s, MIN(l.longitude) w, MAX(l.latitude) nl, MAX(l.longitude) e FROM lodging l WHERE " + f.where(), f.args().toArray());
            total = ((Number) summary.get("n")).longValue();
            bounds = total == 0 ? null : new double[] {((Number) summary.get("s")).doubleValue(), ((Number) summary.get("w")).doubleValue(),
                ((Number) summary.get("nl")).doubleValue(), ((Number) summary.get("e")).doubleValue()};
            String order = "name".equals(q.sort()) ? "l.name, l.lodging_id"
                : "POW(l.latitude-?,2)+POW((l.longitude-?)*0.8,2), l.lodging_id";
            List<Object> args = new ArrayList<>(f.args());
            if (!"name".equals(q.sort())) { args.add(center[0]); args.add(center[1]); }
            args.add(size); args.add((long) page * size);
            ids = jdbc.queryForList("SELECT l.lodging_id FROM lodging l WHERE " + f.where() + " ORDER BY " + order + " LIMIT ? OFFSET ?",
                Long.class, args.toArray());
            avail = ids.isEmpty() ? Map.of() : availability(q, "l.lodging_id IN (" + marks(ids.size()) + ")", new ArrayList<>(ids));
        }
        List<Item> items = items(ids, avail);
        int totalPages = (int) Math.max(1, (total + size - 1) / size);
        return new Page(items, total, page, size, totalPages, bounds);
    }

    /** 지도: 보이는 범위 안 숙소. 120곳이 넘으면 격자로 묶어 개수만 돌려준다. */
    public MapResult map(Query q, double swLat, double swLng, double neLat, double neLng) {
        validate(q);
        if (!validLatitude(swLat) || !validLatitude(neLat) || !validLongitude(swLng) || !validLongitude(neLng)
                || swLat >= neLat || swLng >= neLng)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "지도 범위를 확인해 주세요.");
        Filter f = filter(q, new double[] {swLat, swLng, neLat, neLng});
        String where = f.where();
        List<Object> args = new ArrayList<>(f.args());
        if (q.bookableOnly() || q.maxPrice() != null) {
            Map<Long, Avail> all = availability(q, where, args);
            List<Long> ok = all.entrySet().stream().filter(e -> q.maxPrice() == null || e.getValue().price() <= q.maxPrice()).map(Map.Entry::getKey).toList();
            if (ok.isEmpty()) return new MapResult(List.of(), 0, false);
            where = "l.lodging_id IN (" + marks(ok.size()) + ")";
            args = new ArrayList<>(ok);
        }
        long total = jdbc.queryForObject("SELECT COUNT(*) FROM lodging l WHERE " + where, Long.class, args.toArray());
        if (total <= MAP_POINT_LIMIT) {
            var points = jdbc.query("SELECT l.lodging_id, l.name, l.latitude, l.longitude FROM lodging l WHERE " + where,
                (rs, n) -> new MapPoint(rs.getLong(1), rs.getString(2), rs.getDouble(3), rs.getDouble(4), 1), args.toArray());
            return new MapResult(points, total, false);
        }
        // 현재 보이는 범위를 가로 6 × 세로 8 칸으로 나눠 칸마다 숙소 수를 센다.
        double cellLat = (neLat - swLat) / 8, cellLng = (neLng - swLng) / 6;
        args.add(swLat); args.add(cellLat); args.add(swLng); args.add(cellLng);
        var points = jdbc.query("""
            SELECT COUNT(*) n, AVG(l.latitude) lat, AVG(l.longitude) lng, MIN(l.lodging_id) one, MIN(l.name) name
            FROM lodging l WHERE %s
            GROUP BY LEAST(7, FLOOR((l.latitude - ?) / ?)), LEAST(5, FLOOR((l.longitude - ?) / ?))
            """.formatted(where),
            (rs, n) -> rs.getInt("n") == 1
                ? new MapPoint(rs.getLong("one"), rs.getString("name"), rs.getDouble("lat"), rs.getDouble("lng"), 1)
                : new MapPoint(null, null, rs.getDouble("lat"), rs.getDouble("lng"), rs.getInt("n")), args.toArray());
        return new MapResult(points, total, true);
    }

    /** 시·도 18개와 그 아래 시·군·구 (운영 중 숙소 수 포함, 숙소 없는 곳 제외). */
    public List<Region> regions() {
        Map<Long, Long> counts = new HashMap<>();
        jdbc.query("SELECT region_id, COUNT(*) FROM lodging WHERE status='ACTIVE' AND latitude IS NOT NULL AND longitude IS NOT NULL GROUP BY region_id",
            (org.springframework.jdbc.core.RowCallbackHandler) rs -> counts.put(rs.getLong(1), rs.getLong(2)));
        Map<Long, String> sidoNames = new LinkedHashMap<>();
        Map<Long, List<Region>> children = new HashMap<>();
        jdbc.query("SELECT region_id, region_name, parent_region_id, region_level FROM region ORDER BY region_level DESC, region_name",
            (org.springframework.jdbc.core.RowCallbackHandler) rs -> {
                long id = rs.getLong(1);
                if ("SIDO".equals(rs.getString(4))) sidoNames.put(id, rs.getString(2));
                else if (counts.getOrDefault(id, 0L) > 0)
                    children.computeIfAbsent(rs.getLong(3), k -> new ArrayList<>()).add(new Region(id, rs.getString(2), counts.get(id), List.of()));
            });
        List<Region> result = new ArrayList<>();
        for (var e : sidoNames.entrySet()) {
            List<Region> kids = children.getOrDefault(e.getKey(), List.of());
            long count = counts.getOrDefault(e.getKey(), 0L) + kids.stream().mapToLong(Region::count).sum();
            if (count > 0) result.add(new Region(e.getKey(), e.getValue(), count, kids));
        }
        return result;
    }

    // ---------- 내부 ----------
    private record Filter(String where, List<Object> args) {}
    private record Avail(int price, int remaining) {}

    private void validate(Query q) {
        if (q.checkIn() == null || q.checkOut() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "숙박 날짜를 선택해 주세요.");
        inventory.validateDates(q.checkIn(), q.checkOut());
        if (q.guests() < 1 || q.guests() > 20) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "인원을 확인해 주세요.");
        for (String t : q.types()) if (!TYPES.contains(t)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "숙소 종류를 확인해 주세요.");
        if (q.amenities().size() > 10 || q.amenities().stream().anyMatch(a -> a.isBlank() || a.length() > 30))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "편의시설 조건을 확인해 주세요.");
        if (q.keyword() != null && q.keyword().length() > 50) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "검색어는 50자까지 입력해 주세요.");
        if ((q.centerLat() == null) != (q.centerLng() == null)
                || (q.centerLat() != null && (!validLatitude(q.centerLat()) || !validLongitude(q.centerLng()))))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "지도 중심을 확인해 주세요.");
    }

    /** 숙소 표(l) 조건. 지역은 시·군·구가 있으면 그것, 없으면 시·도와 그 아래 전체. */
    private Filter filter(Query q, double[] bounds) {
        StringBuilder w = new StringBuilder("l.status='ACTIVE' AND l.latitude IS NOT NULL AND l.longitude IS NOT NULL");
        List<Object> args = new ArrayList<>();
        if (q.sigunguId() != null) { w.append(" AND l.region_id=?"); args.add(q.sigunguId()); }
        else if (q.sidoId() != null) {
            w.append(" AND l.region_id IN (SELECT region_id FROM region WHERE region_id=? OR parent_region_id=?)");
            args.add(q.sidoId()); args.add(q.sidoId());
        }
        if (!q.types().isEmpty()) { w.append(" AND l.lodging_type IN (").append(marks(q.types().size())).append(")"); args.addAll(q.types()); }
        for (String a : q.amenities()) { w.append(" AND CONCAT(', ', l.amenities, ',') LIKE ?"); args.add("%, " + like(a.trim()) + ",%"); }
        if (q.keyword() != null && !q.keyword().isBlank()) { w.append(" AND l.name LIKE ?"); args.add("%" + like(q.keyword().trim()) + "%"); }
        if (bounds != null) {
            w.append(" AND l.latitude BETWEEN ? AND ? AND l.longitude BETWEEN ? AND ?");
            args.add(bounds[0]); args.add(bounds[2]); args.add(bounds[1]); args.add(bounds[3]);
        }
        return new Filter(w.toString(), args);
    }

    /** 숙소별 판매 가능 객실의 1박 평균 최저가·남은 객실. 날짜는 정확한 목록(IN)으로 넘겨 (room_id, stay_date) 색인을 쓴다. */
    private Map<Long, Avail> availability(Query q, String where, List<Object> whereArgs) {
        List<LocalDate> nights = q.checkIn().datesUntil(q.checkOut()).toList();
        List<Object> args = new ArrayList<>();
        args.add(q.guests());
        args.addAll(nights);
        args.addAll(whereArgs);
        args.add(nights.size());
        Map<Long, Avail> result = new LinkedHashMap<>();
        jdbc.query("""
            SELECT x.lodging_id, MIN(x.total) total, MAX(x.remaining) remaining FROM (
              SELECT r.lodging_id, r.room_id, SUM(i.price) total,
                     MIN(i.total_quantity - i.held_quantity - i.reserved_quantity) remaining
              FROM lodging l
              JOIN room r ON r.lodging_id=l.lodging_id AND r.is_active=TRUE AND r.max_capacity>=?
              JOIN room_inventory i ON i.room_id=r.room_id AND i.stay_date IN (%s)
                AND i.is_on_sale=TRUE AND i.price>0 AND i.total_quantity - i.held_quantity - i.reserved_quantity > 0
              WHERE %s
              GROUP BY r.lodging_id, r.room_id HAVING COUNT(*)=?
            ) x GROUP BY x.lodging_id
            """.formatted(marks(nights.size()), where),
            (org.springframework.jdbc.core.RowCallbackHandler) rs -> result.put(rs.getLong("lodging_id"),
                new Avail((int) Math.round(rs.getLong("total") / (double) nights.size()), rs.getInt("remaining"))),
            args.toArray());
        return result;
    }

    private double[] center(Query q, Filter f) {
        if (q.centerLat() != null && q.centerLng() != null) return new double[] {q.centerLat(), q.centerLng()};
        var row = jdbc.queryForMap("SELECT AVG(l.latitude) lat, AVG(l.longitude) lng FROM lodging l WHERE " + f.where(), f.args().toArray());
        Object lat = row.get("lat"), lng = row.get("lng");
        return lat == null ? new double[] {36.5, 127.8} : new double[] {((Number) lat).doubleValue(), ((Number) lng).doubleValue()};
    }

    private Map<Long, double[]> coords(List<Long> ids) {
        Map<Long, double[]> map = new HashMap<>();
        for (List<Long> chunk : chunks(ids)) jdbc.query("SELECT lodging_id, latitude, longitude FROM lodging WHERE lodging_id IN (" + marks(chunk.size()) + ")",
            (org.springframework.jdbc.core.RowCallbackHandler) rs -> map.put(rs.getLong(1), new double[] {rs.getDouble(2), rs.getDouble(3)}), chunk.toArray());
        return map;
    }

    /** 이름순(같은 이름은 번호순). 나눠 읽은 뒤 전체를 한 번에 정렬한다. */
    private List<Long> orderedByName(List<Long> ids) {
        Map<Long, String> names = new HashMap<>();
        for (List<Long> chunk : chunks(ids)) jdbc.query("SELECT lodging_id, name FROM lodging WHERE lodging_id IN (" + marks(chunk.size()) + ")",
            (org.springframework.jdbc.core.RowCallbackHandler) rs -> names.put(rs.getLong(1), rs.getString(2)), chunk.toArray());
        List<Long> out = new ArrayList<>(ids);
        out.sort((a, b) -> {
            int c = String.valueOf(names.get(a)).compareTo(String.valueOf(names.get(b)));
            return c != 0 ? c : Long.compare(a, b);
        });
        return out;
    }

    private List<Item> items(List<Long> ids, Map<Long, Avail> avail) {
        if (ids.isEmpty()) return List.of();
        Map<Long, Item> byId = new HashMap<>();
        Map<Long, String> photos = new HashMap<>();
        jdbc.query("SELECT p.lodging_id, COALESCE(NULLIF(p.thumbnail_url, ''), p.image_url) url FROM lodging_photo p WHERE p.lodging_id IN ("
                + marks(ids.size()) + ") AND COALESCE(NULLIF(p.thumbnail_url, ''), NULLIF(p.image_url, '')) IS NOT NULL ORDER BY p.lodging_id, p.sort_order, p.photo_id",
            (org.springframework.jdbc.core.RowCallbackHandler) rs -> photos.putIfAbsent(rs.getLong(1), rs.getString(2)), ids.toArray());
        jdbc.query("SELECT l.lodging_id, l.name, l.address, l.lodging_type, l.latitude, l.longitude, l.amenities, rg.region_name FROM lodging l "
                + "LEFT JOIN region rg ON rg.region_id=l.region_id WHERE l.lodging_id IN (" + marks(ids.size()) + ")",
            (org.springframework.jdbc.core.RowCallbackHandler) rs -> {
                long id = rs.getLong("lodging_id");
                Avail a = avail.get(id);
                String raw = rs.getString("amenities");
                List<String> amenities = raw == null || raw.isBlank() ? List.of()
                    : Arrays.stream(raw.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
                byId.put(id, new Item(id, rs.getString("name"), rs.getString("address"), rs.getString("lodging_type"), rs.getString("region_name"),
                    rs.getObject("latitude", BigDecimal.class), rs.getObject("longitude", BigDecimal.class), photos.get(id), amenities,
                    a != null, a == null ? null : a.price(), a == null ? null : a.remaining()));
            }, ids.toArray());
        return ids.stream().map(byId::get).filter(java.util.Objects::nonNull).toList();
    }

    private static double[] boundsOf(java.util.Collection<double[]> points) {
        if (points.isEmpty()) return null;
        double s = 90, w = 180, n = -90, e = -180;
        for (double[] p : points) { s = Math.min(s, p[0]); w = Math.min(w, p[1]); n = Math.max(n, p[0]); e = Math.max(e, p[1]); }
        return new double[] {s, w, n, e};
    }

    private static double dist(double[] p, double[] c) {
        if (p == null) return Double.MAX_VALUE;
        return Math.pow(p[0] - c[0], 2) + Math.pow((p[1] - c[1]) * 0.8, 2);
    }
    private static boolean validLatitude(double value) { return Double.isFinite(value) && value >= -90 && value <= 90; }
    private static boolean validLongitude(double value) { return Double.isFinite(value) && value >= -180 && value <= 180; }
    private static String marks(int n) { return String.join(",", Collections.nCopies(n, "?")); }
    private static String like(String s) { return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_"); }
    private static List<List<Long>> chunks(List<Long> ids) {
        List<List<Long>> out = new ArrayList<>();
        for (int i = 0; i < ids.size(); i += 1000) out.add(ids.subList(i, Math.min(i + 1000, ids.size())));
        return out;
    }
}
