package com.smarttraveler.dummy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import tools.jackson.databind.ObjectMapper;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DummyDataLoaderTest {
    private final ObjectMapper json = new ObjectMapper();
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final PasswordEncoder passwords = mock(PasswordEncoder.class);
    private final PlatformTransactionManager manager = mock(PlatformTransactionManager.class);
    private final DummyDataLoader loader = new DummyDataLoader(jdbc, passwords, manager, true,
            "jdbc:mariadb://localhost:3306/smart_travel_db");
    private final String fixture = """
            {"source_content_id":"126715","area_code":"51","area_name":"강원특별자치도",
             "name":"예시 숙소","address":"서울특별시 예시","latitude":37.0,"longitude":127.0,
             "lodging_type":"HOTEL","phone":"02-1234-5678","rooms":[{"name":"4인실"}]}
            """;

    @Test void nonLocalDatasourceSkipsAllWrites() {
        DummyDataLoader remote = new DummyDataLoader(jdbc, passwords, manager, true,
                "jdbc:mariadb://db.example.com:3306/smart_travel_db");
        remote.run(null);
        verifyNoInteractions(jdbc, passwords);
        assertFalse(DummyDataLoader.isLocalDatasource("jdbc:mariadb://localhost.evil.test:3306/db"));
        assertFalse(DummyDataLoader.isLocalDatasource("jdbc:mariadb://localhost,db.example.com/db"));
        assertTrue(DummyDataLoader.isLocalDatasource("jdbc:mariadb://localhost:3306/db"));
        assertTrue(DummyDataLoader.isLocalDatasource("jdbc:mariadb://127.0.0.1:3306/db"));
        assertTrue(DummyDataLoader.isLocalDatasource("jdbc:mariadb://[::1]:3306/db"));
    }

    @Test void disabledSkipsEvenLocalDatasource() {
        DummyDataLoader disabled = new DummyDataLoader(jdbc, passwords, manager, false,
                "jdbc:mariadb://localhost:3306/smart_travel_db");
        disabled.run(null);
        verifyNoInteractions(jdbc, passwords);
    }

    @Test void existingRealOwnerIsUntouched() throws Exception {
        stubIds(true, false);
        assertArrayEquals(new int[]{0, 0}, loader.lodging(json.readTree(fixture), 1, "hash"));
        verify(jdbc, never()).update(anyString(), any(Object[].class));
    }

    @Test void existingDummyOwnerGetsLodgingAndOnlyMarkedRoomsUpdated() throws Exception {
        stubIds(true, true);
        loader.lodging(json.readTree(fixture), 1, "hash");
        verify(jdbc).update(contains("UPDATE lodging SET"), any(Object[].class));
        verify(jdbc, times(3)).update(contains("UPDATE room SET"), any(Object[].class));
        verify(jdbc).queryForList(contains("description=? ORDER BY room_id"), eq(Long.class), any(Object[].class));
    }

    @Test void mismatchedDummyOwnerAccountIsNotReused() throws Exception {
        stubIds(false, false);
        assertArrayEquals(new int[]{0, 0}, loader.lodging(json.readTree(fixture), 1, "hash"));
        verify(jdbc, never()).update(contains("INSERT INTO owner_profiles"), any(Object[].class));
        verify(jdbc, never()).update(contains("INSERT INTO lodging"), any(Object[].class));
    }

    @Test void collidingRoomNameSkipsOnlyThatRoom() throws Exception {
        stubIds(true, true);
        AtomicInteger checks = new AtomicInteger();
        when(jdbc.queryForList(contains("room_id<>?"), eq(Long.class), any(Object[].class)))
                .thenAnswer(call -> checks.getAndIncrement() == 0 ? List.of(99L) : List.of());
        loader.lodging(json.readTree(fixture), 1, "hash");
        verify(jdbc).update(contains("UPDATE lodging SET"), any(Object[].class));
        verify(jdbc, times(2)).update(contains("UPDATE room SET"), any(Object[].class));
        assertEquals(3, checks.get());
    }

    @Test void fixedNamesMatchLodgingOrderAndSharedSql() throws Exception {
        Map<String, DummyDataLoader.OwnerName> names = DummyDataLoader.ownerNames();
        assertEquals(145, names.size());
        assertEquals(145, new HashSet<>(names.values().stream().map(DummyDataLoader.OwnerName::name).toList()).size());
        assertEquals("홍길동", names.get("126715").name());
        assertEquals("김민수", names.get("127654").name());
        assertEquals("김철수", names.get("127812").name());
        List<Long> ids = new ArrayList<>();
        try (var stream = getClass().getResourceAsStream("/dummy/lodgings.json")) {
            for (var row : json.readTree(stream)) ids.add(Long.parseLong(row.path("source_content_id").asString()));
        }
        ids.sort(Long::compareTo);
        for (int i = 0; i < ids.size(); i++) assertEquals(i + 1, names.get(ids.get(i).toString()).index());
        String sql = Files.readString(Path.of("../docs/dummy-owner-names.sql"));
        for (Map.Entry<String, DummyDataLoader.OwnerName> entry : names.entrySet()) {
            int index = entry.getValue().index();
            String name = entry.getValue().name();
            assertTrue(sql.contains("-- " + index + " : " + entry.getKey() + " : " + name));
            assertTrue(sql.contains("m.login_id='dummy_owner_" + entry.getKey() + "'"));
            assertTrue(sql.contains("m.login_id='owner" + index + "'"));
            assertTrue(sql.contains("THEN '" + name + "'"));
        }
        assertTrue(new BCryptPasswordEncoder().matches("123456",
                "$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu"));
    }

    @Test void oldDummyOwnerMigratesAndManualNamesStayProtected() throws Exception {
        stubIds(true, true);
        when(jdbc.queryForList(contains("SELECT m.member_id FROM members m JOIN owner_profiles"),
                eq(Long.class), any(Object[].class))).thenReturn(List.of(2L));
        // 목표 아이디(owner1)는 아직 비어 있어야 이행된다 (stubIds는 members 조회를 모두 2L로 돌려줌).
        when(jdbc.queryForList(contains("WHERE login_id=? OR email=?"), eq(Long.class), any(Object[].class)))
                .thenReturn(List.of());
        loader.lodging(json.readTree(fixture), 1, "hash");
        verify(jdbc).update(contains("nickname=CASE WHEN nickname LIKE '숙소 예시 %'"), any(Object[].class));
        verify(jdbc).update(contains("representative_name='예시 업주'"), any(Object[].class));
        verify(jdbc).update(contains("UPDATE lodging SET"), any(Object[].class));
        // Both CASE/WHERE guards leave manually edited values unchanged in SQL.
    }

    @Test void secondRunDoesNotRepeatAccountMigration() throws Exception {
        stubIds(true, true);
        loader.lodging(json.readTree(fixture), 1, "hash");
        verify(jdbc, never()).update(contains("nickname=CASE WHEN nickname LIKE '숙소 예시 %'"), any(Object[].class));
        verify(jdbc, never()).update(contains("UPDATE owner_profiles SET representative_name"), any(Object[].class));
    }

    @Test void takenTargetLoginSkipsMigrationAndLodging() throws Exception {
        stubIds(true, true);
        when(jdbc.queryForList(contains("SELECT m.member_id FROM members m JOIN owner_profiles"),
                eq(Long.class), any(Object[].class))).thenReturn(List.of(2L));
        when(jdbc.queryForList(contains("WHERE login_id=? OR email=?"), eq(Long.class), any(Object[].class)))
                .thenReturn(List.of(99L));
        assertArrayEquals(new int[]{0, 0}, loader.lodging(json.readTree(fixture), 1, "hash"));
        verify(jdbc, never()).update(contains("UPDATE lodging SET"), any(Object[].class));
    }

    @Test void oldDummyUserMigratesOnceAndTakenTargetSkips() {
        when(jdbc.queryForList(contains("role_code='USER'"), eq(Long.class), any(Object[].class)))
                .thenReturn(List.of(8L), List.of(), List.of(8L));
        when(jdbc.queryForList(contains("WHERE login_id=? OR email=?"), eq(Long.class), any(Object[].class)))
                .thenReturn(List.of(), List.of(99L));
        assertTrue(loader.migrateUser(1, "hash"));
        verify(jdbc).update(contains("nickname=CASE WHEN nickname=?"), any(Object[].class));
        clearInvocations(jdbc);
        assertTrue(loader.migrateUser(1, "hash"));
        verify(jdbc, never()).update(anyString(), any(Object[].class));
        assertFalse(loader.migrateUser(1, "hash"));
        verify(jdbc, never()).update(anyString(), any(Object[].class));
    }

    private void stubIds(boolean existingLodging, boolean matchingOwner) {
        when(jdbc.queryForList(anyString(), eq(Long.class), any(Object[].class))).thenAnswer(call -> {
            String sql = call.getArgument(0);
            if (sql.contains("SELECT l.lodging_id")) return matchingOwner ? List.of(7L) : List.of();
            if (sql.contains("SELECT o.owner_id")) return matchingOwner
                    && sql.contains("o.business_number=?")
                    && sql.contains("m.role_code='OWNER'") ? List.of(3L) : List.of();
            if (sql.contains("room_id<>?")) return List.of();
            if (sql.contains("SELECT room_id FROM room")) return List.of(11L, 12L, 13L);
            if (sql.contains("SELECT lodging_id FROM lodging")) return existingLodging ? List.of(7L) : List.of();
            if (sql.contains("SELECT member_id FROM members")) return List.of(2L);
            if (sql.contains("SELECT region_id FROM region")) return List.of(4L);
            return List.of();
        });
    }
}
