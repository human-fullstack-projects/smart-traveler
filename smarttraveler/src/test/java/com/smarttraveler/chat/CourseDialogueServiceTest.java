package com.smarttraveler.chat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** ConditionDialogueService: ai-server(/internal/dialogue)에 메시지를 그대로 전달하고, 이미
 * 추천한 코스 제외 + 반복추천 + 숙소 우선 흐름만 스프링이 처리한다(지역·기간을 단계별로 묻던
 * 기존 다단계 흐름은 ai-server가 메시지 하나로 전부 처리하므로 더 이상 없다). */
class CourseDialogueServiceTest {
    private final JsonMapper mapper = JsonMapper.builder().build();
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final PythonAiClient ai = mock(PythonAiClient.class);
    private final StoredLodgingCandidates lodgings = mock(StoredLodgingCandidates.class);
    private final ConditionDialogueService service = new ConditionDialogueService(jdbc, mapper,
        new ChatMessageBuffer(jdbc, mock(org.springframework.transaction.PlatformTransactionManager.class)), ai, lodgings);

    private ChatMessageBuffer bufferWithRecommendation(long sessionId, String recommendationJson) {
        var buffer = new ChatMessageBuffer(jdbc, mock(org.springframework.transaction.PlatformTransactionManager.class));
        long assistantId = buffer.add(sessionId, "ASSISTANT", "이전 추천");
        buffer.setRecommendation(sessionId, assistantId, recommendationJson);
        return buffer;
    }

    @Test
    void forwardsTheMessageAsIsAndReturnsTheRecommendation() {
        when(ai.process(eq("부산 가고 싶어"), eq(List.of())))
            .thenReturn(new PythonAiClient.Result("추천 1 · 코스 #11", 11L, List.of(11L)));

        var result = service.processResult(1, "부산 가고 싶어", Map.of());

        assertEquals(11L, result.recommendedCourseId());
        assertEquals("추천 1 · 코스 #11", result.message());
        assertNotNull(result.recommendConditions());
    }

    @Test
    void excludesCoursesAlreadyRecommendedInThisSession() {
        var service = new ConditionDialogueService(jdbc, mapper,
            bufferWithRecommendation(1, "{\"sourceCourseId\":11}"), ai, lodgings);
        when(ai.process(eq("다른 코스로 추천해줘"), eq(List.of(11L))))
            .thenReturn(new PythonAiClient.Result("추천 1 · 코스 #12", 12L, List.of(12L)));

        var result = service.processResult(1, "다른 코스로 추천해줘", Map.of());

        assertEquals(12L, result.recommendedCourseId());
    }

    @Test
    void retriesWithoutExclusionWhenNothingIsLeftAndFlagsItRepeated() {
        var service = new ConditionDialogueService(jdbc, mapper,
            bufferWithRecommendation(1, "{\"sourceCourseId\":11}"), ai, lodgings);
        when(ai.process(eq("또 추천해줘"), eq(List.of(11L))))
            .thenReturn(new PythonAiClient.Result("조건에 맞는 저장된 코스를 찾지 못했어요."));
        when(ai.process(eq("또 추천해줘"), eq(List.of())))
            .thenReturn(new PythonAiClient.Result("추천 1 · 코스 #11", 11L, List.of(11L)));

        var result = service.processResult(1, "또 추천해줘", Map.of());

        assertEquals(11L, result.recommendedCourseId());
        assertTrue(result.message().contains("새로 추천할 코스가 더 없어서"));
    }

    @Test
    void doesNotRetryWhenNothingWasExcluded() {
        when(ai.process(eq("부산 가고 싶어"), eq(List.of())))
            .thenReturn(new PythonAiClient.Result("조건에 맞는 저장된 코스를 찾지 못했어요."));

        var result = service.processResult(1, "부산 가고 싶어", Map.of());

        assertNull(result.recommendedCourseId());
        verify(ai, times(1)).process(anyString(), anyList());
    }

    @Test
    void noIdeaAnswerShowsHumanCoursesWithoutCallingAi() {
        when(jdbc.queryForList(contains("human_course_post"))).thenReturn(List.of(Map.of("post_id", 3L, "title", "서울 하루")));

        var result = service.processResult(1, "몰라 아무거나", Map.of());

        assertTrue(result.message().contains("휴먼 추천 코스"));
        assertTrue(result.message().contains("[서울 하루](#/human-courses/3)"));
        verifyNoInteractions(ai);
    }

    @Test
    void lodgingFlowTriesCourseFirstWithASyntheticMessage() {
        when(lodgings.findById(5L)).thenReturn(Map.of("lodgingId", 5L, "name", "바다뷰펜션", "regionId", 6L, "regionName", "부산광역시 해운대구"));
        when(ai.process(eq("숙소: 바다뷰펜션, 지역: 부산광역시 해운대구"), eq(List.of())))
            .thenReturn(new PythonAiClient.Result("추천 1 · 코스 #20", 20L, List.of(20L)));

        var result = service.processResult(1, "아무 메시지", Map.of(), 5L);

        assertEquals(20L, result.recommendedCourseId());
        verify(ai, never()).recommendLodgingItinerary(anyLong(), any());
    }

    @Test
    void lodgingFlowFallsBackToItineraryWhenNoCourseFound() {
        when(lodgings.findById(5L)).thenReturn(Map.of("lodgingId", 5L, "name", "바다뷰펜션", "regionId", 6L, "regionName", "부산광역시 해운대구"));
        when(ai.process(eq("숙소: 바다뷰펜션, 지역: 부산광역시 해운대구"), eq(List.of())))
            .thenReturn(new PythonAiClient.Result("조건에 맞는 저장된 코스를 찾지 못했어요."));
        when(ai.recommendLodgingItinerary(eq(5L), any()))
            .thenReturn(new PythonAiClient.Result("숙소 주변 일정"));

        var result = service.processResult(1, "아무 메시지", Map.of(), 5L);

        assertNull(result.recommendedCourseId());
        assertEquals("숙소 주변 일정", result.message());
    }

    @Test
    void lodgingWithoutRegionNameOmitsTheRegionPhraseFromTheMessage() {
        when(lodgings.findById(5L)).thenReturn(Map.of("lodgingId", 5L, "name", "바다뷰펜션", "regionId", 6L));
        when(ai.process(eq("숙소: 바다뷰펜션"), eq(List.of())))
            .thenReturn(new PythonAiClient.Result("조건에 맞는 저장된 코스를 찾지 못했어요."));
        when(ai.recommendLodgingItinerary(eq(5L), any()))
            .thenReturn(new PythonAiClient.Result("숙소 주변 일정"));

        service.processResult(1, "아무 메시지", Map.of(), 5L);

        verify(ai).process(eq("숙소: 바다뷰펜션"), eq(List.of()));
    }

    @Test
    void missingLodgingAsksToSelectAgain() {
        when(lodgings.findById(99L)).thenReturn(null);

        var result = service.processResult(1, "아무 메시지", Map.of(), 99L);

        assertTrue(result.message().contains("다시 선택"));
        verifyNoInteractions(ai);
    }
}
