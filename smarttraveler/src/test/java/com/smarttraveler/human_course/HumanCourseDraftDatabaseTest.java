package com.smarttraveler.human_course;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.smarttraveler.admin.service.HumanManagementService;
import com.smarttraveler.auth.repository.MemberRepository;
import com.smarttraveler.domain.entity.Member;
import com.smarttraveler.human_course.HumanCourseDtos.PostInput;
import com.smarttraveler.mypage.repository.MyPageRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.ResourcePropertySource;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

@EnabledIfEnvironmentVariable(named = "HUMAN_COURSE_DB_AUDIT", matches = "true")
class HumanCourseDraftDatabaseTest {
    @Test void draftCanBeEditedAndRepublishedWithOriginalEngagement() throws Exception {
        var environment = new StandardEnvironment();
        environment.getPropertySources().addLast(new ResourcePropertySource(new ClassPathResource("application.properties")));
        var dataSource = new DriverManagerDataSource(
            environment.getRequiredProperty("spring.datasource.url"),
            environment.getRequiredProperty("spring.datasource.username"),
            environment.getRequiredProperty("spring.datasource.password"));
        var jdbc = new JdbcTemplate(dataSource);
        var transaction = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        transaction.executeWithoutResult(status -> {
            status.setRollbackOnly();
            verifyDraftLifecycle(jdbc);
        });
    }

    private void verifyDraftLifecycle(JdbcTemplate jdbc) {
        String reservationNo = "TEST-DRAFT-" + UUID.randomUUID();
        assertEquals(1, jdbc.update("""
            INSERT INTO room_reservation(member_id,course_id,room_id,reservation_no,booker_name,booker_phone,
                checkin_date,checkout_date,guest_count,total_price,status)
            SELECT r.member_id,r.course_id,r.room_id,?,'테스트','000-0000-0000',
                r.checkin_date,r.checkout_date,r.guest_count,r.total_price,'COMPLETED'
            FROM room_reservation r JOIN members m ON m.member_id=r.member_id
            WHERE r.status='COMPLETED' AND r.course_id IS NOT NULL AND m.status='ACTIVE'
              AND m.deleted_at IS NULL AND m.role_code='USER'
            ORDER BY r.reservation_id LIMIT 1
            """, reservationNo), "검증에는 코스가 연결된 완료 예약 한 건이 필요합니다.");
        var reservation = jdbc.queryForMap("SELECT reservation_id,member_id,course_id FROM room_reservation WHERE reservation_no=?", reservationNo);
        long reservationId = ((Number) reservation.get("reservation_id")).longValue();
        long memberId = ((Number) reservation.get("member_id")).longValue();
        long courseId = ((Number) reservation.get("course_id")).longValue();
        jdbc.update("""
            INSERT INTO human_course_post(reservation_id,author_id,course_id,title,category,companion_type,content,status,course_snapshot)
            VALUES (?,?,?,'검증 제목','문화','친구','기존 후기','VISIBLE','{"version":1,"plan":[]}')
            """, reservationId, memberId, courseId);
        long postId = jdbc.queryForObject("SELECT post_id FROM human_course_post WHERE reservation_id=?", Long.class, reservationId);
        jdbc.update("INSERT INTO human_course_like(post_id,member_id,created_at) VALUES (?,?,NOW())", postId, memberId);
        jdbc.update("""
            INSERT INTO human_course_comment(post_id,author_id,content,created_at,updated_at)
            VALUES (?,?,'유지되는 댓글',NOW(),NOW())
            """, postId, memberId);

        var member = new Member();
        member.setMemberId(memberId);
        member.setStatus("ACTIVE");
        var members = mock(MemberRepository.class);
        when(members.findByLoginId("draft-audit")).thenReturn(Optional.of(member));
        var auth = UsernamePasswordAuthenticationToken.authenticated("draft-audit", null, List.of());
        var mapper = new ObjectMapper();
        var myPage = new MyPageRepository(jdbc, mapper);
        var service = new HumanCourseService(jdbc, mapper, members, mock(HumanCoursePhotoStore.class), new HumanManagementService(jdbc));

        var before = service.detail(postId, auth);
        assertEquals(postId, service.unpublish(auth, postId).id());
        var draft = service.detail(postId, auth);
        assertEquals("DRAFT", draft.visibility());
        assertEquals(before.body(), draft.body());
        assertEquals(before.comments(), draft.comments());
        assertEquals(1L, draft.likes());
        assertEquals("DRAFT", myPage.posts(memberId).stream().filter(post -> post.id().equals(Long.toString(postId))).findFirst().orElseThrow().status());
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(ResponseStatusException.class, () -> service.detail(postId, null)).getStatusCode());

        var edited = new PostInput("수정 제목", "힐링", "가족", null, "수정한 여행 후기", List.of(), false);
        assertEquals(postId, service.edit(auth, postId, edited, true).id());
        var published = service.detail(postId, null);
        assertEquals("PUBLIC", published.visibility());
        assertEquals(edited.title(), published.title());
        assertEquals(edited.body(), published.body());
        assertEquals(edited.category(), published.category());
        assertEquals(edited.companion(), published.companion());
        assertEquals(draft.comments().getFirst().id(), published.comments().getFirst().id());
        assertEquals(draft.comments().getFirst().text(), published.comments().getFirst().text());
        assertEquals(draft.likes(), published.likes());
    }
}
