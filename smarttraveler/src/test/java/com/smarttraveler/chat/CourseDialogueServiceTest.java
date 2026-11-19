package com.smarttraveler.chat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CourseDialogueServiceTest {
    @Test @SuppressWarnings("unchecked")
    void allConfirmationPromptsReceiveCandidatesAndPreferences() {
        for (boolean enabled : List.of(true, false)) {
            var mapper = JsonMapper.builder().build();
            var jdbc = mock(JdbcTemplate.class);
            var ai = mock(PythonAiClient.class);
            var places = mock(StoredPlaceCandidates.class);
            var courses = mock(StoredCourseCandidates.class);
            var service = new ConditionDialogueService(jdbc, mapper, ai, places, courses, enabled);
            var conditions = mapper.createObjectNode();
            conditions.put("regionName", "부산"); conditions.put("durationDays", 3);
            conditions.put("startDate", "2026-12-10"); conditions.put("adults", 2); conditions.put("children", 0);
            conditions.put("companionType", "FRIENDS"); conditions.put("transportType", "CAR");
            var rows = List.of(Map.<String, Object>of("courseId", 1L));
            when(courses.find(any())).thenReturn(rows);
            when(places.find(any())).thenReturn(rows);
            when(ai.process(anyString(), anyString(), any(), anyList(), nullable(List.class), anyMap()))
                .thenReturn(new PythonAiClient.Result("추천 결과", true));
            for (String prompt : List.of("입력된 여행 조건을 확인해 주세요.",
                    "입력된 여행 조건을 확인하기 전에, 바꾸고 싶은 내용을 말씀해 주세요.", "모호한 답변 뒤 재확인")) {
                when(jdbc.query(contains("conditions_json"), any(RowMapper.class), eq(1L)))
                    .thenReturn(List.of(mapper.writeValueAsString(conditions)));
                when(jdbc.query(contains("SELECT content"), any(RowMapper.class), eq(1L))).thenReturn(List.of(prompt));
                assertEquals("추천 결과", service.process(1, "네", Map.of("pace", "relaxed")));
                verify(ai).process(eq("네"), eq(prompt), any(ObjectNode.class), eq(enabled ? List.of() : rows),
                    eq(enabled ? rows : null), eq(Map.of("pace", "relaxed")));
            }
            if (enabled) { verify(courses, times(3)).find(any()); verifyNoInteractions(places); }
            else { verify(places, times(3)).find(any()); verifyNoInteractions(courses); }
        }
    }
    @Test @SuppressWarnings("unchecked")
    void noStoredCourseFallsBackToPlaceCandidates() {
        var mapper = JsonMapper.builder().build();
        var jdbc = mock(JdbcTemplate.class);
        var ai = mock(PythonAiClient.class);
        var places = mock(StoredPlaceCandidates.class);
        var courses = mock(StoredCourseCandidates.class);
        var service = new ConditionDialogueService(jdbc, mapper, ai, places, courses, true);
        var conditions = mapper.createObjectNode();
        conditions.put("regionName", "부산"); conditions.put("durationDays", 5);
        conditions.put("startDate", "2026-12-10"); conditions.put("adults", 2); conditions.put("children", 0);
        conditions.put("companionType", "FRIENDS"); conditions.put("transportType", "CAR");
        var placeRows = List.of(Map.<String, Object>of("placeId", 7L));
        when(courses.find(any())).thenReturn(List.of());
        when(places.find(any())).thenReturn(placeRows);
        when(ai.process(anyString(), anyString(), any(), anyList(), nullable(List.class), anyMap()))
            .thenReturn(new PythonAiClient.Result("관광지 추천", true));
        when(jdbc.query(contains("conditions_json"), any(RowMapper.class), eq(1L)))
            .thenReturn(List.of(mapper.writeValueAsString(conditions)));
        when(jdbc.query(contains("SELECT content"), any(RowMapper.class), eq(1L))).thenReturn(List.of("입력된 여행 조건을 확인해 주세요."));
        assertEquals("관광지 추천", service.process(1, "네", Map.of()));
        // 코스 후보가 없으면 courseCandidates=null(관광지 경로)과 관광지 후보를 함께 넘긴다.
        verify(ai).process(eq("네"), anyString(), any(ObjectNode.class), eq(placeRows), isNull(), eq(Map.of()));
    }
    @Test @SuppressWarnings("unchecked")
    void incompleteConditionsDoNotQueryCandidates() {
        var jdbc = mock(JdbcTemplate.class);
        var ai = mock(PythonAiClient.class);
        var courses = mock(StoredCourseCandidates.class);
        var places = mock(StoredPlaceCandidates.class);
        when(jdbc.query(anyString(), any(RowMapper.class), eq(1L))).thenReturn(List.of());
        when(ai.process(anyString(), anyString(), any(), anyList(), anyList(), anyMap()))
            .thenReturn(new PythonAiClient.Result("지역을 알려주세요", false));
        new ConditionDialogueService(jdbc, JsonMapper.builder().build(), ai, places, courses, true).process(1, "추천해줘");
        verifyNoInteractions(courses, places);
    }
}
