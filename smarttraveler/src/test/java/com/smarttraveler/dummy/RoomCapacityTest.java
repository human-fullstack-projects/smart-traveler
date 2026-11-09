package com.smarttraveler.dummy;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import static org.junit.jupiter.api.Assertions.*;

class RoomCapacityTest {
    private final ObjectMapper json = new ObjectMapper();

    @Test void fourPersonRoom() throws Exception {
        RoomCapacity.Spec room = RoomCapacity.from(json.readTree("{\"name\":\"4인실\"}"), "객실", 2, 2);
        assertEquals(2, room.base());
        assertEquals(4, room.max());
    }

    @Test void rangeUsesLowerAndUpperBounds() throws Exception {
        RoomCapacity.Spec room = RoomCapacity.from(json.readTree("{\"name\":\"2~4인\"}"), "객실", 2, 2);
        assertEquals(2, room.base());
        assertEquals(4, room.max());
    }

    @Test void noNumberUsesDefaultAndLabelsIt() throws Exception {
        RoomCapacity.Spec room = RoomCapacity.from(json.readTree("{\"name\":\"스탠다드 더블\"}"), "객실", 2, 4);
        assertEquals(2, room.base());
        assertEquals(4, room.max());
        assertTrue(room.name().contains("4인"));
    }

    @Test void detailCountsOverrideNameWithoutContradiction() throws Exception {
        RoomCapacity.Spec room = RoomCapacity.from(json.readTree(
                "{\"name\":\"8인실\",\"base_capacity\":\"8\",\"max_capacity\":\"10\"}"), "객실", 2, 4);
        assertEquals(8, room.base());
        assertEquals(10, room.max());
        assertTrue(room.name().contains("10인"));
        assertTrue(room.name().contains("8인실"));
    }
}
