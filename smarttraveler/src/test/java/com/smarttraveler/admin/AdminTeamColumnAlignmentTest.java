package com.smarttraveler.admin;

import com.smarttraveler.admin.dto.AdminViews;
import com.smarttraveler.domain.entity.HumanCoursePost;
import com.smarttraveler.domain.entity.HumanCourseReport;
import com.smarttraveler.domain.entity.Member;
import com.smarttraveler.domain.entity.RoomReservation;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AdminTeamColumnAlignmentTest {
    @Test
    void adminViewsKeepLegacyNamesWithoutSerializingRelations() {
        RoomReservation reservation = new RoomReservation();
        reservation.setReservationId(42L);
        reservation.setMemberId(7L);
        Map<String, Object> json = AdminViews.of(reservation);
        assertEquals(42L, json.get("reservationId"));
        assertEquals(7L, json.get("memberId"));
        assertFalse(json.containsKey("member"));
        assertFalse(json.containsKey("room"));

        HumanCoursePost post = new HumanCoursePost();
        post.setPostId(5L);
        post.setAuthorId(7L);
        Member author = new Member();
        author.setNickname("여행 작가");
        author.setLoginId("writer7");
        AdminViews.HumanCourseView view = new AdminViews.HumanCourseView(post, "Gyeongju", true, author);
        assertEquals(5L, view.getId());
        assertEquals(7L, view.getMemberId());
        assertEquals("여행 작가", view.getAuthorNickname());
        assertEquals("Gyeongju", view.getDestination());
        assertTrue(view.getFeatured());

        author.setNickname("  ");
        assertEquals("writer7", new AdminViews.HumanCourseView(post, "Gyeongju", false, author).getAuthorNickname());

        HumanCourseReport report = new HumanCourseReport();
        report.setReportId(9L);
        report.setPostId(5L);
        AdminViews.HumanReportView reportView = new AdminViews.HumanReportView(report, author);
        assertEquals(9L, reportView.getId());
        assertEquals(5L, reportView.getCourseId());
        assertEquals("writer7", reportView.getReporterNickname());
    }
}
