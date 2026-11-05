package com.smarttraveler.admin;

import com.smarttraveler.admin.dto.AdminViews;
import com.smarttraveler.domain.entity.HumanCoursePost;
import com.smarttraveler.domain.entity.HumanCourseReport;
import com.smarttraveler.domain.entity.RoomReservation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AdminTeamColumnAlignmentTest {
    @Test
    void adminViewsKeepLegacyNamesWithoutSerializingRelations() {
        var reservation = new RoomReservation();
        reservation.setReservationId(42L);
        reservation.setMemberId(7L);
        var json = AdminViews.of(reservation);
        assertEquals(42L, json.get("reservationId"));
        assertEquals(7L, json.get("memberId"));
        assertFalse(json.containsKey("member"));
        assertFalse(json.containsKey("room"));

        var post = new HumanCoursePost();
        post.setPostId(5L);
        post.setAuthorId(7L);
        var view = new AdminViews.HumanCourseView(post, "Gyeongju", true);
        assertEquals(5L, view.getId());
        assertEquals(7L, view.getMemberId());
        assertEquals("Gyeongju", view.getDestination());
        assertTrue(view.getFeatured());

        var report = new HumanCourseReport();
        report.setReportId(9L);
        report.setPostId(5L);
        var reportView = new AdminViews.HumanReportView(report);
        assertEquals(9L, reportView.getId());
        assertEquals(5L, reportView.getCourseId());
    }
}
