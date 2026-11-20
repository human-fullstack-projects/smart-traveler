package com.smarttraveler.chat;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** 피드백 반영: 조건 1개여도 바로 추천 · 「몰라」면 휴먼 추천 코스 안내 · 이미 추천한 코스 제외. */
class CourseDialogueServiceTest {
    private final JsonMapper mapper = JsonMapper.builder().build();
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final PythonAiClient ai = mock(PythonAiClient.class);
    private final StoredPlaceCandidates places = mock(StoredPlaceCandidates.class);
    private final StoredCourseCandidates courses = mock(StoredCourseCandidates.class);
    private final ConditionDialogueService service = new ConditionDialogueService(jdbc, mapper,
        new ChatMessageBuffer(jdbc, mock(org.springframework.transaction.PlatformTransactionManager.class)), ai, places, courses, true);

    @SuppressWarnings("unchecked")
    private void stored(ObjectNode conditions) {
        when(jdbc.query(contains("conditions_json"), any(RowMapper.class), eq(1L)))
            .thenReturn(conditions == null ? List.of() : List.of(mapper.writeValueAsString(conditions)));
    }

    private void extractReturns(String question, Map<String, Object> changes) {
        when(ai.process(anyString(), any(), any(), anyList(), isNull(), anyMap(), eq(PythonAiClient.Mode.EXTRACT_ONLY)))
            .thenAnswer(call -> {
                ObjectNode target = call.getArgument(2);
                changes.forEach((key, value) -> target.set(key, mapper.valueToTree(value)));
                return new PythonAiClient.Result(question, false);
            });
    }

    @Test
    void oneConditionRecommendsRightAwayWithDefaults() {
        stored(null);
        extractReturns("아직 알려주지 않은 정보가 있어요", Map.of("regionName", "부산"));
        when(places.regionId(any())).thenReturn(6L);
        var rows = List.of(Map.<String, Object>of("courseId", 11L));
        when(courses.find(any(), any())).thenReturn(rows);
        when(ai.process(anyString(), any(), any(), anyList(), any(), anyMap(), eq(PythonAiClient.Mode.FORCE_RECOMMEND)))
            .thenReturn(new PythonAiClient.Result("추천 1 · 코스 #11", true, 11L));

        var result = service.processResult(1, "부산 가고 싶어", Map.of());

        assertEquals(11L, result.recommendedCourseId());
        assertTrue(result.message().contains("추천 1 · 코스 #11"));
        assertTrue(result.message().startsWith("말씀하지 않은 조건은 기간 1박 2일"), result.message());
        assertTrue(result.message().contains("동행·교통은 상관없음"), result.message());
        ArgumentCaptor<ObjectNode> used = ArgumentCaptor.forClass(ObjectNode.class);
        verify(ai).process(eq("부산 가고 싶어"), any(), used.capture(), eq(List.of()), eq(rows), anyMap(), eq(PythonAiClient.Mode.FORCE_RECOMMEND));
        assertEquals(2, used.getValue().path("durationDays").asInt());
        assertEquals(1, used.getValue().path("adults").asInt());
        assertFalse(used.getValue().path("startDate").asText().isBlank());
        assertEquals("부산", result.recommendConditions().path("regionName").asText());
        verify(jdbc).update(contains("confirmed=TRUE"), eq(1L));
    }

    @Test
    void noStoredCourseFallsBackToPlaceCandidates() {
        stored(null);
        extractReturns("질문", Map.of("regionName", "부산", "durationDays", 2));
        when(places.regionId(any())).thenReturn(6L);
        when(courses.find(any(), any())).thenReturn(List.of());
        var placeRows = List.of(Map.<String, Object>of("placeId", 7L));
        when(places.find(any())).thenReturn(placeRows);
        when(ai.process(anyString(), any(), any(), anyList(), any(), anyMap(), eq(PythonAiClient.Mode.FORCE_RECOMMEND)))
            .thenReturn(new PythonAiClient.Result("관광지 추천", true));
        assertTrue(service.processResult(1, "부산 2일", Map.of()).message().endsWith("관광지 추천"));
        verify(ai).process(anyString(), any(), any(), eq(placeRows), isNull(), anyMap(), eq(PythonAiClient.Mode.FORCE_RECOMMEND));
    }

    @Test
    void noConditionAndNoIdeaShowsHumanCourses() {
        stored(null);
        extractReturns("여행 일정을 추천해 드리기 위해", Map.of());
        when(jdbc.queryForList(contains("human_course_post"))).thenReturn(List.of(Map.of("post_id", 3L, "title", "서울 하루")));
        var result = service.processResult(1, "몰라 아무거나", Map.of());
        assertTrue(result.message().contains("휴먼 추천 코스"));
        assertTrue(result.message().contains("[서울 하루](#/human-courses/3)"));
        assertTrue(result.message().contains("(#/human-courses)"));
        verify(ai, never()).process(anyString(), any(), any(), anyList(), any(), anyMap(), eq(PythonAiClient.Mode.FORCE_RECOMMEND));
        verifyNoInteractions(courses);
    }

    @Test
    void noConditionOtherwiseAsksTheGuideQuestion() {
        stored(null);
        extractReturns("여행 일정을 추천해 드리기 위해 몇가지 정보를 요청할게요.", Map.of());
        assertEquals("여행 일정을 추천해 드리기 위해 몇가지 정보를 요청할게요.", service.processResult(1, "안녕", Map.of()).message());
        verifyNoInteractions(courses, places);
    }

    @Test
    void unknownRegionNameIsReportedInsteadOfNationwide() {
        stored(null);
        extractReturns("질문", Map.of("regionName", "아틀란티스"));
        when(places.regionId(any())).thenReturn(null);
        assertTrue(service.processResult(1, "아틀란티스 가자", Map.of()).message().contains("지역을 찾지 못했어요"));
        verifyNoInteractions(courses);
    }

    @Test
    void fillDefaultsKeepsWhatTheUserSaid() {
        ObjectNode c = mapper.createObjectNode();
        c.put("regionName", "제주"); c.put("durationDays", 3); c.put("startDate", "2026-12-10");
        c.put("companionType", "FAMILY"); c.put("transportType", "CAR");
        String note = ConditionDialogueService.fillDefaults(c);
        assertEquals(3, c.path("durationDays").asInt());
        assertEquals("2026-12-10", c.path("startDate").asText());
        assertEquals(2, c.path("adults").asInt());
        assertEquals(0, c.path("children").asInt());
        assertEquals("말씀하지 않은 조건은 어른 2명으로 잡았어요. 바꾸고 싶으면 말씀하시거나 오른쪽에서 바꿀 수 있어요.", note);
        assertFalse(ConditionDialogueService.hasAnyCondition(mapper.createObjectNode()));
        assertTrue(ConditionDialogueService.NO_IDEA.matcher("글쎄요 딱히").find());
        assertFalse(ConditionDialogueService.NO_IDEA.matcher("부산 가고 싶어").find());
        assertTrue(Set.of().isEmpty());
    }
}
