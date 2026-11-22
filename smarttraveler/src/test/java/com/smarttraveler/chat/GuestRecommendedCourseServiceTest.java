package com.smarttraveler.chat;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class GuestRecommendedCourseServiceTest {
    JdbcTemplate jdbc = mock(JdbcTemplate.class);
    RecommendedCourseService courses = mock(RecommendedCourseService.class);
    PlatformTransactionManager manager = mock(PlatformTransactionManager.class);
    GuestRecommendedCourseService service = new GuestRecommendedCourseService(jdbc, courses, manager, new ChatMessageBuffer(jdbc, manager));
    MockHttpSession session = new MockHttpSession();
    String snapshot = """
        {"title":"부산 코스","destination":"부산","conditions":{"startDate":"2026-12-10","durationDays":2}}
        """;

    @BeforeEach @SuppressWarnings("unchecked") void setup() {
        when(courses.read(anyString())).thenAnswer(call -> JsonMapper.builder().build().readTree(call.getArgument(0, String.class)));
        when(jdbc.query(contains("SELECT m.recommendation_json"), any(RowMapper.class), anyLong(), anyLong())).thenReturn(List.of(snapshot));
        when(jdbc.query(contains("SELECT member_id"), any(RowMapper.class), anyLong())).thenReturn(Collections.singletonList(null));
        when(manager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        when(courses.save(7L, 1L, 10L)).thenReturn(99L);
    }
    @Test void newestRecommendationWinsAndExpiredSessionHasNoPreview() {
        service.store(session, 1, 10);
        service.store(session, 1, 9);
        assertEquals(10, service.status(session, null).messageId());
        service.store(session, 1, 11);
        assertEquals(11, service.status(session, null).messageId());
        session.invalidate();
        assertNull(service.status(new MockHttpSession(), null));
        verifyNoInteractions(manager);
    }
    @Test void finalCommitFailureKeepsPendingCourseAndBindsTheFirstMember() {
        service.store(session, 1, 10);
        doThrow(new IllegalStateException("commit failed")).doNothing().when(manager).commit(any());
        assertThrows(IllegalStateException.class, () -> service.claim(session, 7));
        assertNotNull(service.status(session, 7L));
        assertNull(service.status(session, 8L));
        assertThrows(ResponseStatusException.class, () -> service.claim(session, 8));
        assertEquals(99L, service.claim(session, 7));
        assertNull(service.status(session, 7L));
        assertEquals(99L, service.claim(session, 7));
        assertNull(service.claim(session, 8));
        verify(courses, times(2)).save(7L, 1L, 10L);
    }
    @Test void databaseFailureKeepsPendingAndSuccessfulRetryClearsIt() {
        service.store(session, 1, 10);
        when(courses.save(7L, 1L, 10L)).thenThrow(new IllegalStateException("insert failed")).thenReturn(99L);
        assertThrows(IllegalStateException.class, () -> service.claim(session, 7));
        verify(manager).rollback(any());
        assertNotNull(service.status(session, 7L));
        assertEquals(99L, service.claim(session, 7));
        assertNull(service.status(session, 7L));
    }
}
