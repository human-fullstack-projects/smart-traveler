package com.smarttraveler.chat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ConditionDialogueTest {
    private JdbcTemplate jdbc;
    private RuleBasedConditionExtractor extractor;
    private final ConditionQuestions questions = new ConditionQuestions();
    private ObjectNode conditions;
    @BeforeEach
    @SuppressWarnings("unchecked")
    void 준비() {
        jdbc = mock(JdbcTemplate.class);
        when(jdbc.query(anyString(), any(RowMapper.class))).thenReturn(List.of());
        when(jdbc.queryForObject(anyString(), eq(Integer.class), any())).thenReturn(1);
        extractor = new RuleBasedConditionExtractor(jdbc);
        conditions = JsonMapper.builder().build().createObjectNode();
        conditions.putNull("regionId"); conditions.put("startDate", ""); conditions.putNull("durationDays");
        conditions.putNull("adults"); conditions.putNull("children"); conditions.put("companionType", ""); conditions.put("transportType", "");
        conditions.put("flowType", "COURSE_FIRST");
    }
    @Test void 명확한_표현을_합치고_부족한_조건을_질문한다() {
        extractor.extract("지역 번호 1, 2026-12-10 출발, 친구와 2박 3일, 성인 2명", "", conditions);
        assertEquals(1, conditions.path("regionId").asInt());
        assertEquals("2026-12-10", conditions.path("startDate").asText());
        assertEquals(3, conditions.path("durationDays").asInt());
        assertEquals("FRIENDS", conditions.path("companionType").asText());
        assertEquals(2, conditions.path("adults").asInt());
        assertEquals(ConditionQuestions.CHILDREN, questions.next(conditions));
        extractor.extract("없어요", ConditionQuestions.CHILDREN, conditions);
        assertEquals(0, conditions.path("children").asInt());
        assertEquals(ConditionQuestions.TRANSPORT, questions.next(conditions));
        extractor.extract("자차", ConditionQuestions.TRANSPORT, conditions);
        assertEquals(ConditionQuestions.CHECK, questions.next(conditions));
    }
    @Test void 모호한_인원은_성인이나_아동으로_추측하지_않는다() {
        extractor.extract("친구 2명이랑 가요", "", conditions);
        assertTrue(conditions.path("adults").isNull());
        assertTrue(conditions.path("children").isNull());
        assertEquals("FRIENDS", conditions.path("companionType").asText());
    }
    @Test void 일반_일정의_날짜를_출발일로_추측하지_않는다() {
        extractor.extract("2026-12-10에 공연을 볼래요", "", conditions);
        assertEquals("", conditions.path("startDate").asText());
        extractor.extract("2026-12-10", ConditionQuestions.DATE, conditions);
        assertEquals("2026-12-10", conditions.path("startDate").asText());
    }
    @Test void 확인한_기존_값은_새_정보가_없으면_유지한다() {
        conditions.put("durationDays", 3); conditions.put("adults", 2);
        extractor.extract("좋아요", "", conditions);
        assertEquals(3, conditions.path("durationDays").asInt());
        assertEquals(2, conditions.path("adults").asInt());
    }
    @Test void 잘못된_날짜와_일수는_반영하지_않는다() {
        extractor.extract("2026-02-30 출발, 2박 7일", "", conditions);
        assertEquals("", conditions.path("startDate").asText());
        assertTrue(conditions.path("durationDays").isNull());
    }
    @Test void 등록되지_않은_지역_번호는_반영하지_않는다() {
        when(jdbc.queryForObject(anyString(), eq(Integer.class), any())).thenReturn(0);
        extractor.extract("지역 번호 99999", "", conditions);
        assertTrue(conditions.path("regionId").isNull());
        assertEquals(ConditionQuestions.REGION, questions.next(conditions));
    }
}
