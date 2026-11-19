package com.smarttraveler.stay;

import static org.junit.jupiter.api.Assertions.*;

import com.smarttraveler.stay.service.StayInventoryService;
import com.smarttraveler.stay.service.StaySearchService;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

/** 숙소 찾기 검색·지도를 실제 DB(공유 DB 사본 등)로 확인한다. STAY_DB_AUDIT=true + DB_URL로 켤 때만 돈다. */
@EnabledIfEnvironmentVariable(named = "STAY_DB_AUDIT", matches = "true")
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
class StaySearchDatabaseTest {
    @Autowired StaySearchService search;
    @Autowired StayInventoryService inventory;
    @Autowired JdbcTemplate jdbc;

    final LocalDate checkIn = LocalDate.now(ZoneId.of("Asia/Seoul")).plusDays(10);
    final LocalDate checkOut = checkIn.plusDays(2);

    StaySearchService.Query query(Long sido, String sort, boolean bookableOnly, Integer maxPrice, List<String> types) {
        return new StaySearchService.Query(sido, null, types, List.of(), null, checkIn, checkOut, 2, maxPrice, bookableOnly, sort, null, null);
    }

    long busan() { return jdbc.queryForObject("SELECT region_id FROM region WHERE region_level='SIDO' AND region_name LIKE '부산%'", Long.class); }

    @Test void regionFilterAndPagingMatchDatabase() {
        long sido = busan();
        long expected = jdbc.queryForObject("""
            SELECT COUNT(*) FROM lodging WHERE status='ACTIVE' AND latitude IS NOT NULL AND longitude IS NOT NULL
              AND region_id IN (SELECT region_id FROM region WHERE region_id=? OR parent_region_id=?)""", Long.class, sido, sido);
        var first = search.search(query(sido, "distance", false, null, List.of()), 0, 5);
        assertEquals(expected, first.total());
        assertEquals(first.total(), search.regions().stream().filter(r -> r.regionId() == sido).findFirst().orElseThrow().count(),
            "지역 선택칸 숫자와 검색 결과가 같음");
        assertEquals(5, first.items().size());
        assertEquals((expected + 4) / 5, first.totalPages());
        var second = search.search(query(sido, "distance", false, null, List.of()), 1, 5);
        assertTrue(first.items().stream().noneMatch(a -> second.items().stream().anyMatch(b -> b.lodgingId().equals(a.lodgingId()))), "쪽끼리 겹치지 않음");
        first.items().forEach(item -> assertTrue(item.address().startsWith("부산"), item.address()));
    }

    @Test void listPriceEqualsRoomScreenPrice() {
        var page = search.search(query(busan(), "price", true, null, List.of()), 0, 10);
        assertFalse(page.items().isEmpty());
        int previous = 0;
        for (var item : page.items()) {
            assertTrue(item.saleable());
            assertTrue(item.minPrice() >= previous, "가격 오름차순");
            previous = item.minPrice();
            var rooms = inventory.rooms(item.lodgingId(), checkIn, checkOut, 2);
            int real = (int) Math.round(rooms.stream().mapToInt(StayInventoryService.RoomOption::totalPrice).min().orElseThrow() / 2.0);
            assertEquals(real, item.minPrice(), item.name());
        }
    }

    @Test void maxPriceAndTypeFilters() {
        var page = search.search(query(busan(), "distance", false, 90000, List.of("PENSION", "CAMPING")), 0, 10);
        page.items().forEach(item -> {
            assertTrue(item.saleable() && item.minPrice() <= 90000, item.name());
            assertTrue(List.of("PENSION", "CAMPING").contains(item.lodgingType()));
        });
    }

    @Test void mapClustersAddUpToTotal() {
        var all = search.map(query(null, "distance", false, null, List.of()), 33.0, 124.5, 38.7, 131.0);
        assertTrue(all.clustered());
        assertTrue(all.points().size() <= 48, "지도 격자는 최대 6×8칸");
        assertEquals(all.total(), all.points().stream().mapToLong(p -> p.count()).sum());
        var small = search.map(query(null, "distance", false, null, List.of()), 35.158, 129.158, 35.162, 129.165);
        assertFalse(small.clustered());
        small.points().forEach(p -> assertEquals(1, p.count()));
    }

    @Test void regionsCoverEveryActiveLodging() {
        long active = jdbc.queryForObject("SELECT COUNT(*) FROM lodging WHERE status='ACTIVE' AND latitude IS NOT NULL AND longitude IS NOT NULL", Long.class);
        var regions = search.regions();
        assertEquals(active, regions.stream().mapToLong(r -> r.count()).sum());
        assertTrue(regions.size() >= 17);
    }

    @Test void rejectsBadInput() {
        assertThrows(ResponseStatusException.class, () -> search.search(query(null, "distance", false, null, List.of("CASTLE")), 0, 10));
        assertThrows(ResponseStatusException.class, () -> search.search(query(null, "distance", false, null, List.of()), 0, 500));
        assertThrows(ResponseStatusException.class, () -> search.map(query(null, "distance", false, null, List.of()), 36, 128, 35, 127));
        assertThrows(ResponseStatusException.class, () -> search.map(query(null, "distance", false, null, List.of()), Double.NaN, 128, 38, 130));
        assertTrue(search.search(query(busan(), "price", true, null, List.of()), Integer.MAX_VALUE, 10).items().isEmpty(),
            "큰 페이지 번호는 오류 없이 빈 목록");
    }
}
