package com.smarttraveler.dummy;

import java.util.List;
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
    private final DummyDataLoader loader = new DummyDataLoader(jdbc, mock(PasswordEncoder.class),
            mock(PlatformTransactionManager.class), true);
    private final String fixture = """
            {"source_content_id":"123","area_code":"11","area_name":"서울특별시",
             "name":"예시 숙소","address":"서울특별시 예시","latitude":37.0,"longitude":127.0,
             "lodging_type":"HOTEL","phone":"02-1234-5678","rooms":[{"name":"4인실"}]}
            """;

    @Test void existingRealOwnerIsUntouched() throws Exception {
        stubIds(false);
        assertArrayEquals(new int[]{0, 0}, loader.lodging(json.readTree(fixture), 1, "hash"));
        verify(jdbc, never()).update(anyString(), any(Object[].class));
    }

    @Test void existingDummyOwnerGetsLodgingAndOnlyMarkedRoomsUpdated() throws Exception {
        stubIds(true);
        loader.lodging(json.readTree(fixture), 1, "hash");
        verify(jdbc).update(contains("UPDATE lodging SET"), any(Object[].class));
        verify(jdbc, times(3)).update(contains("UPDATE room SET"), any(Object[].class));
        verify(jdbc).queryForList(contains("description=? ORDER BY room_id"), eq(Long.class), any(Object[].class));
    }

    private void stubIds(boolean dummyOwner) {
        when(jdbc.queryForList(anyString(), eq(Long.class), any(Object[].class))).thenAnswer(call -> {
            String sql = call.getArgument(0);
            if (sql.contains("JOIN owner_profiles o")) return dummyOwner ? List.of(7L) : List.of();
            if (sql.contains("SELECT room_id FROM room")) return List.of(11L, 12L, 13L);
            if (sql.contains("SELECT lodging_id FROM lodging")) return List.of(7L);
            if (sql.contains("SELECT member_id FROM members")) return List.of(2L);
            if (sql.contains("SELECT owner_id FROM owner_profiles")) return List.of(3L);
            if (sql.contains("SELECT region_id FROM region")) return List.of(4L);
            return List.of();
        });
    }
}
