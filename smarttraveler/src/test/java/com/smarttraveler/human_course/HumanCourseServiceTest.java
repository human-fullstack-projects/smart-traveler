package com.smarttraveler.human_course;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.smarttraveler.admin.service.HumanManagementService;
import com.smarttraveler.auth.repository.MemberRepository;
import com.smarttraveler.domain.entity.Member;
import com.smarttraveler.human_course.HumanCourseDtos.Day;
import com.smarttraveler.human_course.HumanCourseDtos.MapPlace;
import com.smarttraveler.human_course.HumanCourseDtos.PostInput;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

class HumanCourseServiceTest {
    @Test void copiedRouteKeepsVisitOrderAndCoordinatesWithoutItsOriginalLodging() {
        var beach = new MapPlace("1", 5L, "광안리", "부산 수영구", new BigDecimal("35.15"), new BigDecimal("129.12"), "course", List.of());
        var park = new MapPlace("2", 6L, "수변공원", null, null, null, "course", List.of());
        var stay = new MapPlace("stay-3", null, "원래 숙소", null, null, null, "stay", List.of());
        var plan = List.of(new Day(1, List.of(), List.of(beach, stay, park), "2025-02-01"),
                new Day(3, List.of("부산시립미술관"), List.of(), "2025-02-03"));
        var start = LocalDate.of(2026, 12, 10);
        var days = HumanCourseService.itineraryDays(plan, start);

        assertEquals(List.of(start, start.plusDays(1), start.plusDays(2)), days.stream().map(day -> day.date()).toList());
        assertEquals(List.of("광안리", "수변공원"), days.getFirst().places());
        assertEquals(35.15, days.getFirst().mapPlaces().getFirst().latitude());
        assertTrue(days.get(1).places().isEmpty());
        assertEquals(List.of("부산시립미술관"), days.getLast().places());
    }

    @Test void itineraryLookupExcludesHiddenAndDeletedPosts() {
        var jdbc = mock(JdbcTemplate.class);
        var service = new HumanCourseService(jdbc, mock(ObjectMapper.class), mock(MemberRepository.class),
                mock(HumanCoursePhotoStore.class), mock(HumanManagementService.class));
        var error = assertThrows(ResponseStatusException.class, () -> service.itinerary(3L, LocalDate.of(2026, 12, 10)));
        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        verify(jdbc).queryForList(contains("p.status='VISIBLE' AND p.deleted_at IS NULL"), eq(3L));
    }
    @Test void homeReviewsIncludesGuestsAndSortsLikesThenActiveComments() {
        var jdbc = mock(JdbcTemplate.class);
        var service = new HumanCourseService(jdbc, mock(ObjectMapper.class), mock(MemberRepository.class),
            mock(HumanCoursePhotoStore.class), mock(HumanManagementService.class));
        assertTrue(service.homeReviews(null).isEmpty());
        var sql = ArgumentCaptor.forClass(String.class);
        verify(jdbc).queryForList(sql.capture(), eq(Long.class));
        String query = sql.getValue();
        assertTrue(query.contains("p.status = 'VISIBLE'"));
        assertTrue(query.contains("p.deleted_at IS NULL"));
        assertTrue(query.contains("co.deleted_at IS NULL"));
        assertTrue(query.indexOf("human_course_like") < query.indexOf("human_course_comment"));
        assertTrue(query.contains("p.created_at DESC, p.post_id DESC"));
        assertTrue(query.contains("LIMIT 3"));
    }

    @Test void eligibilityRequiresCompletedStayAndFinalCourse() {
        var now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        var yesterday = now.minusDays(1);
        assertEquals("", HumanCourseService.eligibility("COMPLETED", yesterday, true, false));
        assertFalse(HumanCourseService.eligibility("CONFIRMED", yesterday, true, false).isEmpty());
        assertFalse(HumanCourseService.eligibility("COMPLETED", now.plusDays(1), true, false).isEmpty());
        assertFalse(HumanCourseService.eligibility("COMPLETED", yesterday, false, false).isEmpty());
        assertFalse(HumanCourseService.eligibility("COMPLETED", yesterday, true, true).isEmpty());
    }

    @Test void listUsesStayPhotoOnlyWhenThereIsNoVisitPhoto() {
        var jdbc = mock(JdbcTemplate.class);
        var service = new HumanCourseService(jdbc, mock(ObjectMapper.class), mock(MemberRepository.class),
            mock(HumanCoursePhotoStore.class), mock(HumanManagementService.class));
        when(jdbc.queryForList(contains("SELECT post_id FROM human_course_post")))
            .thenReturn(List.of(Map.of("post_id", 1L)));
        when(jdbc.queryForList(contains("WHERE p.post_id=?"), eq(-1L), eq(-1L), eq(1L)))
            .thenReturn(List.of(Map.of("author_id", 7L, "status", "VISIBLE", "course_id", 3L,
                "start_date", "2026-11-01", "end_date", "2026-11-02", "lock_version", 1,
                "content", "후기", "created_at", "2026-11-03 12:00:00", "likes", 0,
                "stay_image_url", "https://example.test/stay.jpg")));

        assertEquals("https://example.test/stay.jpg", service.list(null).getFirst().coverPhotoUrl());

        when(jdbc.queryForList(contains("SELECT image.image_url FROM course_day"), eq(String.class), eq(3L)))
            .thenReturn(List.of("https://example.test/visit.jpg"));
        assertEquals("https://example.test/visit.jpg", service.list(null).getFirst().coverPhotoUrl());
    }

    @Test void anonymousCannotCreateOrDelete() {
        var jdbc = mock(JdbcTemplate.class);
        var service = new HumanCourseService(jdbc, mock(ObjectMapper.class), mock(MemberRepository.class),
            mock(HumanCoursePhotoStore.class), mock(HumanManagementService.class));
        var input = new PostInput("제목", "문화", "친구", "소개", "후기", List.of(), false);
        assertEquals(HttpStatus.UNAUTHORIZED, assertThrows(ResponseStatusException.class,
            () -> service.create(null, 1, input)).getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED, assertThrows(ResponseStatusException.class,
            () -> service.delete(null, 1)).getStatusCode());
        verifyNoInteractions(jdbc);
    }

    @Test void memberCannotCreatePostForAnotherMembersReservation() {
        var jdbc = mock(JdbcTemplate.class);
        var members = mock(MemberRepository.class);
        var account = new Member();
        account.setMemberId(7L);
        account.setStatus("ACTIVE");
        when(members.findByLoginId("owner")).thenReturn(Optional.of(account));
        var auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getName()).thenReturn("owner");
        var service = new HumanCourseService(jdbc, mock(ObjectMapper.class), members,
            mock(HumanCoursePhotoStore.class), mock(HumanManagementService.class));
        var input = new PostInput("제목", "문화", "친구", null, "후기", List.of(), false);
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(ResponseStatusException.class,
            () -> service.create(auth, 3, input)).getStatusCode());
        verify(jdbc).queryForList(contains("member_id=? FOR UPDATE"), eq(3L), eq(7L));
    }
}
