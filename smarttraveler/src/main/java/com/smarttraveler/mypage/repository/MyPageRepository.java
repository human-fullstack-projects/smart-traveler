package com.smarttraveler.mypage.repository;

import com.smarttraveler.mypage.dto.MyPageDtos.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.ObjectMapper;

@Repository
public class MyPageRepository {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    public MyPageRepository(JdbcTemplate jdbc, ObjectMapper mapper) {
        this.jdbc = jdbc; this.mapper = mapper;
    }

    public boolean lockMember(long memberId) {
        return !jdbc.queryForList("SELECT member_id FROM members WHERE member_id=? AND status='ACTIVE' AND deleted_at IS NULL FOR UPDATE", Long.class, memberId).isEmpty();
    }

    public void importDraft(long memberId, Draft draft) {
        // 같은 회원의 동일 clientId 재전송은 기존 내용을 덮어쓰지 않는다.
        jdbc.update("""
            INSERT INTO mypage_course_draft(member_id, client_id, title, region, start_date, end_date, days_json)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE client_id=VALUES(client_id)
            """, memberId, draft.clientId(), draft.title().trim(), draft.region().trim(),
                draft.startDate(), draft.endDate(), mapper.writeValueAsString(draft.days()));
    }

    public List<CourseView> courses(long memberId) {
        List<CourseView> result = new ArrayList<>(jdbc.query("""
            SELECT id, title, region, start_date, end_date, days_json
            FROM mypage_course_draft WHERE member_id=? ORDER BY id DESC
            """, (rs, n) -> new CourseView("draft-" + rs.getLong("id"), rs.getString("title"),
                rs.getString("region"), rs.getObject("start_date", LocalDate.class),
                rs.getObject("end_date", LocalDate.class), "DRAFT", readDays(rs.getString("days_json"))), memberId));
        result.addAll(jdbc.query("""
            SELECT id, COALESCE(title, destination) AS title, destination, start_date, end_date, status
            FROM course WHERE member_no=? AND deleted_at IS NULL AND is_active=TRUE ORDER BY id DESC
            """, (rs, n) -> new CourseView("course-" + rs.getLong("id"), rs.getString("title"),
                rs.getString("destination"), rs.getObject("start_date", LocalDate.class),
                rs.getObject("end_date", LocalDate.class), rs.getString("status"), List.of()), memberId));
        return result;
    }

    public CourseView course(long memberId, String id) {
        return courses(memberId).stream().filter(c -> c.id().equals(id)).findFirst().map(c -> {
            if (!id.startsWith("course-")) return c;
            var days = jdbc.query("""
                SELECT d.id, d.day_no, d.visit_date FROM course_day d
                JOIN course c ON c.id=d.course_id
                WHERE c.id=? AND c.member_no=? AND c.deleted_at IS NULL AND c.is_active=TRUE ORDER BY d.day_no
                """, (rs, n) -> {
                    LocalDate date = rs.getObject("visit_date", LocalDate.class);
                    var places = jdbc.queryForList("SELECT place_name FROM activity WHERE course_day_id=? ORDER BY seq", String.class, rs.getLong("id"));
                    return new Day(date == null ? c.startDate().plusDays(rs.getInt("day_no") - 1L) : date, places);
                }, Long.parseLong(id.substring(7)), memberId);
            return new CourseView(c.id(), c.title(), c.region(), c.startDate(), c.endDate(), c.state(), days);
        }).orElse(null);
    }

    public List<BookingView> bookings(long memberId) {
        return jdbc.query("""
            SELECT r.*, ro.name AS room_name, l.name AS stay_name,
                   CASE WHEN c.member_no=? AND c.deleted_at IS NULL AND c.is_active=TRUE THEN c.id ELSE NULL END AS owned_course_id
            FROM room_reservation r LEFT JOIN room ro ON ro.room_id=r.room_id
            LEFT JOIN lodging l ON l.lodging_id=ro.lodging_id LEFT JOIN course c ON c.id=r.course_id
            WHERE r.member_id=? ORDER BY r.created_at DESC, r.reservation_id DESC
            """, (rs, n) -> new BookingView(String.valueOf(rs.getLong("reservation_id")), rs.getString("reservation_no"),
                rs.getObject("owned_course_id") == null ? null : "course-" + rs.getLong("owned_course_id"),
                rs.getString("stay_name"), rs.getString("room_name"), rs.getObject("checkin_date", LocalDate.class),
                rs.getObject("checkout_date", LocalDate.class), rs.getInt("guest_count"), rs.getInt("total_price"),
                rs.getString("status")), memberId, memberId);
    }

    public List<PostView> posts(long memberId) {
        return jdbc.query("""
            SELECT post_id, course_id, reservation_id, title, content, status, course_snapshot
            FROM human_course_post WHERE author_id=? AND deleted_at IS NULL ORDER BY created_at DESC
            """, (rs, n) -> new PostView(String.valueOf(rs.getLong("post_id")), "course-" + rs.getLong("course_id"),
                String.valueOf(rs.getLong("reservation_id")), rs.getString("title"), rs.getString("content"),
                rs.getString("status"), rs.getString("course_snapshot")), memberId);
    }

    private List<Day> readDays(String json) {
        return List.of(mapper.readValue(json, Day[].class));
    }
}
