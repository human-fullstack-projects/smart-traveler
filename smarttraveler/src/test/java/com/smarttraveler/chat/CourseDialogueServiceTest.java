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
    private final StoredLodgingCandidates lodgings = mock(StoredLodgingCandidates.class);
    private final ConditionDialogueService service = new ConditionDialogueService(jdbc, mapper,
        new ChatMessageBuffer(jdbc, mock(org.springframework.transaction.PlatformTransactionManager.class)), ai, places, courses, lodgings, true);

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
        when(ai.process(anyString(), any(), any(), anyList(), any(), anyMap(), eq(PythonAiClient.Mode.FORCE_RECOMMEND), anyBoolean()))
            .thenReturn(new PythonAiClient.Result("추천 1 · 코스 #11", true, 11L));

        var result = service.processResult(1, "부산 가고 싶어", Map.of());

        assertEquals(11L, result.recommendedCourseId());
        assertTrue(result.message().contains("추천 1 · 코스 #11"));
        assertTrue(result.message().startsWith("입력하신 조건: 지역\n말씀하지 않은 조건은 기간 1박 2일 · 어른 1명으로 잡았어요"), result.message());
        assertTrue(result.message().contains("동행·교통은 상관없음"), result.message());
        assertTrue(result.message().contains("높은 품질의 답변을 원하시면 추가 조건을 입력해 주세요."), result.message());
        assertTrue(result.message().contains("출발 날짜 정보"));
        assertFalse(result.message().contains("여행 지역 정보"));
        ArgumentCaptor<ObjectNode> used = ArgumentCaptor.forClass(ObjectNode.class);
        verify(ai).process(eq("부산 가고 싶어"), any(), used.capture(), eq(List.of()), eq(rows), anyMap(), eq(PythonAiClient.Mode.FORCE_RECOMMEND), anyBoolean());
        assertEquals(2, used.getValue().path("durationDays").asInt());
        assertEquals(1, used.getValue().path("adults").asInt());
        assertTrue(used.getValue().path("startDate").asText().isBlank());
        assertEquals("부산", result.recommendConditions().path("regionName").asText());
        verify(jdbc).update(contains("confirmed=TRUE"), eq(1L));
    }

    @Test
    void companionAloneSatisfiesHeadcountWithoutDefaultNote() {
        stored(null);
        extractReturns("질문", Map.of("regionName", "부산", "companionType", "FRIENDS"));
        when(places.regionId(any())).thenReturn(6L);
        var rows = List.of(Map.<String, Object>of("courseId", 12L));
        when(courses.find(any(), any())).thenReturn(rows);
        when(ai.process(anyString(), any(), any(), anyList(), any(), anyMap(), eq(PythonAiClient.Mode.FORCE_RECOMMEND), anyBoolean()))
            .thenReturn(new PythonAiClient.Result("추천 1 · 코스 #12", true, 12L));

        var result = service.processResult(1, "친구랑 부산 갈래", Map.of());

        assertTrue(result.message().contains("입력하신 조건: 지역, 동행, 인원수"), result.message());
        assertFalse(result.message().contains("어른 1명"), result.message());
        assertEquals(1, result.recommendConditions().path("adults").asInt());
    }

    @Test
    void showAllMatchesIsTrueOnlyWhenDurationAndHeadcountAreBothMissing() {
        stored(null);
        extractReturns("질문", Map.of("regionName", "부산"));
        when(places.regionId(any())).thenReturn(6L);
        var rows = List.of(Map.<String, Object>of("courseId", 30L));
        when(courses.find(any(), any())).thenReturn(rows);
        when(ai.process(anyString(), any(), any(), anyList(), any(), anyMap(), eq(PythonAiClient.Mode.FORCE_RECOMMEND), anyBoolean()))
            .thenReturn(new PythonAiClient.Result("추천 1 · 코스 #30", true, 30L));

        service.processResult(1, "부산 가고 싶어", Map.of());

        verify(ai).process(anyString(), any(), any(), anyList(), eq(rows), anyMap(), eq(PythonAiClient.Mode.FORCE_RECOMMEND), eq(true));
    }

    @Test
    void showAllMatchesIsFalseWhenHeadcountGivenViaCompanion() {
        stored(null);
        extractReturns("질문", Map.of("regionName", "부산", "companionType", "FRIENDS"));
        when(places.regionId(any())).thenReturn(6L);
        var rows = List.of(Map.<String, Object>of("courseId", 31L));
        when(courses.find(any(), any())).thenReturn(rows);
        when(ai.process(anyString(), any(), any(), anyList(), any(), anyMap(), eq(PythonAiClient.Mode.FORCE_RECOMMEND), anyBoolean()))
            .thenReturn(new PythonAiClient.Result("추천 1 · 코스 #31", true, 31L));

        service.processResult(1, "친구랑 부산 갈래", Map.of());

        verify(ai).process(anyString(), any(), any(), anyList(), eq(rows), anyMap(), eq(PythonAiClient.Mode.FORCE_RECOMMEND), eq(false));
    }

    @Test
    void noStoredCourseFallsBackToPlaceCandidates() {
        stored(null);
        extractReturns("질문", Map.of("regionName", "부산", "durationDays", 2));
        when(places.regionId(any())).thenReturn(6L);
        when(courses.find(any(), any())).thenReturn(List.of());
        var placeRows = List.of(Map.<String, Object>of("placeId", 7L));
        when(places.find(any())).thenReturn(placeRows);
        when(ai.process(anyString(), any(), any(), anyList(), any(), anyMap(), eq(PythonAiClient.Mode.FORCE_RECOMMEND), anyBoolean()))
            .thenReturn(new PythonAiClient.Result("관광지 추천", true));
        assertTrue(service.processResult(1, "부산 2일", Map.of()).message().contains("관광지 추천"));
        verify(ai).process(anyString(), any(), any(), eq(placeRows), isNull(), anyMap(), eq(PythonAiClient.Mode.FORCE_RECOMMEND), anyBoolean());
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
        verify(ai, never()).process(anyString(), any(), any(), anyList(), any(), anyMap(), eq(PythonAiClient.Mode.FORCE_RECOMMEND), anyBoolean());
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
    void lodgingNameResolvesWhenOneMatch() {
        stored(null);
        extractReturns("질문", Map.of("lodgingName", "바다뷰펜션"));
        when(lodgings.findByName("바다뷰펜션")).thenReturn(List.of(
            Map.of("lodgingId", 5L, "name", "바다뷰펜션", "address", "부산 해운대구", "regionId", 6L)));
        when(lodgings.findById(5L)).thenReturn(Map.of("lodgingId", 5L, "name", "바다뷰펜션", "address", "부산 해운대구", "regionId", 6L));
        var rows = List.of(Map.<String, Object>of("courseId", 20L));
        when(courses.find(any(), any())).thenReturn(rows);
        when(ai.process(anyString(), any(), any(), anyList(), any(), anyMap(), eq(PythonAiClient.Mode.FORCE_RECOMMEND), anyBoolean()))
            .thenReturn(new PythonAiClient.Result("추천 1 · 코스 #20", true, 20L));

        var result = service.processResult(1, "바다뷰펜션 근처로 코스 짜줘", Map.of());

        assertEquals(20L, result.recommendedCourseId());
        ArgumentCaptor<ObjectNode> used = ArgumentCaptor.forClass(ObjectNode.class);
        verify(ai).process(anyString(), any(), used.capture(), anyList(), eq(rows), anyMap(), eq(PythonAiClient.Mode.FORCE_RECOMMEND), anyBoolean());
        assertEquals(6L, used.getValue().path("regionId").asLong());
    }

    @Test
    void lodgingNameAsksWhenMultipleMatches() {
        stored(null);
        extractReturns("질문", Map.of("lodgingName", "바다뷰"));
        when(lodgings.findByName("바다뷰")).thenReturn(List.of(
            Map.of("lodgingId", 5L, "name", "바다뷰펜션", "address", "부산 해운대구", "regionId", 6L),
            Map.of("lodgingId", 6L, "name", "바다뷰모텔", "address", "속초 중앙동", "regionId", 9L)));

        var result = service.processResult(1, "바다뷰 근처로 코스 짜줘", Map.of());

        assertTrue(result.message().contains("바다뷰펜션") && result.message().contains("바다뷰모텔"));
        verify(ai, never()).process(anyString(), any(), any(), anyList(), any(), anyMap(), eq(PythonAiClient.Mode.FORCE_RECOMMEND), anyBoolean());
    }

    @Test
    void lodgingNameAsksAgainWhenNoMatch() {
        stored(null);
        extractReturns("질문", Map.of("lodgingName", "없는숙소"));
        when(lodgings.findByName("없는숙소")).thenReturn(List.of());

        var result = service.processResult(1, "없는숙소 근처로 코스 짜줘", Map.of());

        assertTrue(result.message().contains("찾지 못했어요"));
    }

    @Test
    void bookingUsesSelectedLodgingAndKeepsItForLaterMessages() {
        ObjectNode selected = mapper.createObjectNode();
        stored(null);
        when(lodgings.findById(21L)).thenReturn(Map.of(
            "name", "같은 이름 숙소", "regionId", 17L, "regionName", "부산광역시 해운대구"));
        when(places.regionId(any())).thenReturn(17L);
        var rows = List.of(Map.<String, Object>of("courseId", 20L));
        when(courses.find(any(), any())).thenReturn(rows);
        when(ai.process(anyString(), any(), any(), anyList(), eq(rows), anyMap(), eq(PythonAiClient.Mode.FORCE_RECOMMEND), anyBoolean()))
            .thenReturn(new PythonAiClient.Result("추천 1 · 코스 #20", true, 20L));

        var first = service.processResult(1, "숙소: 같은 이름 숙소, 지역: 다른 지역", Map.of(), 21L);

        assertEquals(20L, first.recommendedCourseId(), first.message());
        assertEquals(17L, first.recommendConditions().path("regionId").asLong());
        assertEquals(2, first.recommendConditions().path("durationDays").asInt());
        var saved = ArgumentCaptor.forClass(String.class);
        verify(jdbc).update(contains("INSERT INTO chat_session_conditions"), eq(1L), saved.capture());
        selected.setAll((ObjectNode) mapper.readTree(saved.getValue()));
        assertEquals(21L, selected.path("lodgingId").asLong());
        verify(lodgings, never()).findByName(anyString());

        stored(selected);
        extractReturns("질문", Map.of("durationDays", 3));
        var next = service.processResult(1, "3일로 바꿔줘", Map.of());
        assertEquals(21L, next.recommendConditions().path("lodgingId").asLong());
        assertEquals(3, next.recommendConditions().path("durationDays").asInt());
    }

    @Test
    void bookingWithNoRegionUsesNearbyPlaces() {
        stored(null);
        var latitude = java.math.BigDecimal.valueOf(35);
        var longitude = java.math.BigDecimal.valueOf(129);
        when(lodgings.findById(21L)).thenReturn(Map.of("name", "숙소", "latitude", latitude, "longitude", longitude));
        var nearby = List.of(Map.<String, Object>of("placeId", 1L), Map.<String, Object>of("placeId", 2L), Map.<String, Object>of("placeId", 3L));
        when(places.findNearLodging(latitude, longitude)).thenReturn(nearby);
        when(ai.process(anyString(), any(), any(), eq(nearby), isNull(), anyMap(), eq(PythonAiClient.Mode.FORCE_RECOMMEND), anyBoolean()))
            .thenReturn(new PythonAiClient.Result("숙소 주변 일정", true));

        assertTrue(service.processResult(1, "숙소: 숙소", Map.of(), 21L).message().contains("숙소 주변 일정"));
        verify(courses, never()).find(any(), any());
    }

    @Test
    void missingBookingLodgingAsksToSelectAgain() {
        stored(null);
        when(lodgings.findById(21L)).thenReturn(null);
        assertTrue(service.processResult(1, "숙소: 없는 숙소", Map.of(), 21L).message().contains("다시 선택"));
        verify(lodgings, never()).findByName(anyString());
    }

    @Test
    void durationNotRequiredWhenLodgingBased() {
        stored(null);
        extractReturns("질문", Map.of("lodgingName", "바다뷰펜션"));
        when(lodgings.findByName("바다뷰펜션")).thenReturn(List.of(
            Map.of("lodgingId", 5L, "name", "바다뷰펜션", "address", "부산 해운대구", "regionId", 6L)));
        when(lodgings.findById(5L)).thenReturn(Map.of("lodgingId", 5L, "name", "바다뷰펜션", "address", "부산 해운대구", "regionId", 6L));
        var rows = List.of(Map.<String, Object>of("courseId", 20L));
        when(courses.find(any(), any())).thenReturn(rows);
        when(ai.process(anyString(), any(), any(), anyList(), any(), anyMap(), eq(PythonAiClient.Mode.FORCE_RECOMMEND), anyBoolean()))
            .thenReturn(new PythonAiClient.Result("추천 1 · 코스 #20", true, 20L));

        var result = service.processResult(1, "바다뷰펜션 근처로 코스 짜줘", Map.of());

        assertEquals(20L, result.recommendedCourseId());
        assertTrue(result.message().contains("출발 날짜 정보"));
    }

    @Test
    void fillDefaultsKeepsWhatTheUserSaid() {
        ObjectNode c = mapper.createObjectNode();
        c.put("regionName", "제주"); c.put("durationDays", 3); c.put("startDate", "2026-12-10");
        c.put("companionType", "FAMILY"); c.put("transportType", "CAR");
        String note = ConditionDialogueService.fillDefaults(c);
        assertEquals(3, c.path("durationDays").asInt());
        assertEquals("2026-12-10", c.path("startDate").asText());
        assertEquals(1, c.path("adults").asInt());
        assertEquals(0, c.path("children").asInt());
        assertEquals("입력하신 조건: 지역, 출발일, 기간, 동행, 인원수, 교통", note);
        assertFalse(ConditionDialogueService.hasAnyCondition(mapper.createObjectNode()));
        assertTrue(ConditionDialogueService.NO_IDEA.matcher("글쎄요 딱히").find());
        assertFalse(ConditionDialogueService.NO_IDEA.matcher("부산 가고 싶어").find());
        assertTrue(Set.of().isEmpty());
    }
}
