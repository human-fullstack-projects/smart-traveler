package com.smarttraveler.dummy;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
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
            {"source_content_id":"123","area_code":"11","area_name":"서울특별시",
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

    private void stubIds(boolean existingLodging, boolean matchingOwner) {
        when(jdbc.queryForList(anyString(), eq(Long.class), any(Object[].class))).thenAnswer(call -> {
            String sql = call.getArgument(0);
            if (sql.contains("SELECT l.lodging_id")) return matchingOwner ? List.of(7L) : List.of();
            if (sql.contains("SELECT o.owner_id")) return matchingOwner
                    && sql.contains("o.business_number=?")
                    && sql.contains("o.representative_name='예시 업주'") ? List.of(3L) : List.of();
            if (sql.contains("room_id<>?")) return List.of();
            if (sql.contains("SELECT room_id FROM room")) return List.of(11L, 12L, 13L);
            if (sql.contains("SELECT lodging_id FROM lodging")) return existingLodging ? List.of(7L) : List.of();
            if (sql.contains("SELECT member_id FROM members")) return List.of(2L);
            if (sql.contains("SELECT region_id FROM region")) return List.of(4L);
            return List.of();
        });
    }
}
