package com.smarttraveler.human_course;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.smarttraveler.admin.service.HumanManagementService;
import com.smarttraveler.auth.repository.MemberRepository;
import com.smarttraveler.domain.entity.Member;
import com.smarttraveler.human_course.HumanCourseDtos.PostInput;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

class HumanCourseServiceTest {
    @Test void eligibilityRequiresCompletedStayAndFinalCourse() {
        var now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        var yesterday = now.minusDays(1);
        assertEquals("", HumanCourseService.eligibility("COMPLETED", yesterday, true, false));
        assertFalse(HumanCourseService.eligibility("CONFIRMED", yesterday, true, false).isEmpty());
        assertFalse(HumanCourseService.eligibility("COMPLETED", now.plusDays(1), true, false).isEmpty());
        assertFalse(HumanCourseService.eligibility("COMPLETED", yesterday, false, false).isEmpty());
        assertFalse(HumanCourseService.eligibility("COMPLETED", yesterday, true, true).isEmpty());
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
        var input = new PostInput("제목", "문화", "친구", "소개", "후기", List.of(), false);
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(ResponseStatusException.class,
            () -> service.create(auth, 3, input)).getStatusCode());
        verify(jdbc).queryForList(contains("member_id=? FOR UPDATE"), eq(3L), eq(7L));
    }
}
