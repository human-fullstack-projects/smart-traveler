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
                SELECT d.id, d.day_no, d.visit_date, c.condition_meta FROM course_day d
                JOIN course c ON c.id=d.course_id
                WHERE c.id=? AND c.member_no=? AND c.deleted_at IS NULL AND c.is_active=TRUE ORDER BY d.day_no
                """, (rs, n) -> {
                    LocalDate date = rs.getObject("visit_date", LocalDate.class);
                    var places = jdbc.queryForList("SELECT place_name FROM activity WHERE course_day_id=? ORDER BY seq", String.class, rs.getLong("id"));
                    if (places.isEmpty() && rs.getString("condition_meta") != null) {
                        places = new ArrayList<>();
                        for (var day : mapper.readTree(rs.getString("condition_meta")).path("itinerary")) {
                            if (day.path("day_no").asInt() == rs.getInt("day_no")) {
                                for (var visit : day.path("visits")) places.add(visit.path("place_name").asText());
                            }
                        }
                    }
                    var mapPlaces = jdbc.query("""
                        SELECT p.place_id, a.place_name AS name,
                               COALESCE(a.address_snapshot,p.address) AS address,
                               COALESCE(a.latitude_snapshot,p.latitude) AS latitude,
                               COALESCE(a.longitude_snapshot,p.longitude) AS longitude
                        FROM activity a LEFT JOIN place p ON p.place_id=a.place_id
                        WHERE a.course_day_id=? ORDER BY a.seq
                        """, (point, i) -> new MapPlace(point.getObject("place_id", Long.class), point.getString("name"),
                            point.getString("address"), point.getObject("latitude", Double.class),
                            point.getObject("longitude", Double.class)), rs.getLong("id"));
                    return new Day(date == null ? c.startDate().plusDays(rs.getInt("day_no") - 1L) : date, places, mapPlaces);
                }, Long.parseLong(id.substring(7)), memberId);
            return new CourseView(c.id(), c.title(), c.region(), c.startDate(), c.endDate(), c.state(), days);
        }).orElse(null);
    }

    public List<BookingView> bookings(long memberId) {
        return jdbc.query("""
            WITH owned_reservations AS (
            SELECT r.*, ro.name AS booked_room_name, l.lodging_id AS booked_lodging_id,
                   l.source_provider AS booked_provider, l.name AS booked_stay_name,
                   COALESCE(NULLIF(TRIM(CASE WHEN c.member_no=r.member_id THEN c.destination END),''),
                            NULLIF(TRIM(draft.region),'')) AS match_region,
                   CASE WHEN c.member_no=? AND c.deleted_at IS NULL AND c.is_active=TRUE THEN CONCAT('course-',c.id)
                        WHEN draft.id IS NOT NULL THEN CONCAT('draft-',draft.id) ELSE NULL END AS owned_course_id
            FROM room_reservation r LEFT JOIN room ro ON ro.room_id=r.room_id
            LEFT JOIN lodging l ON l.lodging_id=ro.lodging_id LEFT JOIN course c ON c.id=r.course_id
            LEFT JOIN mypage_course_draft draft ON draft.member_id=r.member_id
              AND draft.id=JSON_UNQUOTE(JSON_EXTRACT(CASE WHEN JSON_VALID(r.request_memo) THEN r.request_memo ELSE '{}' END,'$.draftId'))
            WHERE r.member_id=?
            ), lodging_matches AS (
                SELECT owned.*, CASE WHEN booked_provider='TEST' OR booked_stay_name LIKE '[테스트]%'
                    THEN (SELECT candidate.lodging_id FROM lodging candidate
                          LEFT JOIN region area ON area.region_id=candidate.region_id
                          WHERE candidate.status='ACTIVE' AND candidate.source_provider<>'TEST'
                            AND candidate.name NOT LIKE '[테스트]%'
                            AND match_region IS NOT NULL
                            AND (candidate.address LIKE CONCAT(match_region,'%')
                              OR candidate.address LIKE CONCAT('% ',match_region,' %')
                              OR candidate.address LIKE CONCAT('% ',match_region,'시 %')
                              OR candidate.address LIKE CONCAT('% ',match_region,'군 %')
                              OR (match_region IN ('광주','광주광역시') AND candidate.address REGEXP
                                  '^전남광주통합특별시 (동구|서구|남구|북구|광산구) ')
                              OR area.region_name=match_region OR area.sigungu_name=match_region
                              OR area.sido_name LIKE CONCAT(match_region,'%'))
                          ORDER BY EXISTS(SELECT 1 FROM lodging_photo photo
                              WHERE photo.lodging_id=candidate.lodging_id
                                AND COALESCE(NULLIF(photo.image_url,''),NULLIF(photo.thumbnail_url,'')) IS NOT NULL) DESC,
                              candidate.lodging_id LIMIT 1)
                    ELSE booked_lodging_id END AS display_lodging_id
                FROM owned_reservations owned
            )
            SELECT matched.*, display_stay.name AS stay_name, display_stay.address AS stay_address,
                   display_stay.phone AS stay_phone, display_stay.description AS stay_description,
                   display_stay.checkin_time AS stay_checkin_time, display_stay.checkout_time AS stay_checkout_time,
                   CASE WHEN display_lodging_id=booked_lodging_id THEN booked_room_name
                        ELSE (SELECT room.name FROM room WHERE room.lodging_id=display_lodging_id
                              AND room.is_active=TRUE AND room.max_capacity>=matched.guest_count
                              ORDER BY room.max_capacity,room.room_id LIMIT 1) END AS room_name,
                   (SELECT COALESCE(NULLIF(photo.image_url,''),NULLIF(photo.thumbnail_url,''))
                    FROM lodging_photo photo WHERE photo.lodging_id=display_lodging_id
                    AND COALESCE(NULLIF(photo.image_url,''),NULLIF(photo.thumbnail_url,'')) IS NOT NULL
                    ORDER BY photo.sort_order,photo.photo_id LIMIT 1) AS stay_image_url
            FROM lodging_matches matched LEFT JOIN lodging display_stay ON display_stay.lodging_id=display_lodging_id
            ORDER BY matched.created_at DESC, matched.reservation_id DESC
            """, (rs, n) -> new BookingView(String.valueOf(rs.getLong("reservation_id")), rs.getString("reservation_no"),
                rs.getString("owned_course_id"),
                rs.getString("stay_name"), rs.getString("room_name"), rs.getObject("checkin_date", LocalDate.class),
                rs.getObject("checkout_date", LocalDate.class), rs.getInt("guest_count"), rs.getInt("total_price"),
                rs.getString("status"), rs.getObject("display_lodging_id") == null ? null : new StayInfo(
                    rs.getLong("display_lodging_id"), rs.getString("stay_address"), rs.getString("stay_phone"),
                    rs.getString("stay_description"), rs.getString("stay_image_url"), rs.getString("stay_checkin_time"),
                    rs.getString("stay_checkout_time"), rs.getLong("display_lodging_id") != rs.getLong("booked_lodging_id"))), memberId, memberId);
    }

    public List<PostView> posts(long memberId) {
        return jdbc.query("""
            SELECT post_id, course_id, reservation_id, title, content, status, course_snapshot
            FROM human_course_post WHERE author_id=? AND deleted_at IS NULL ORDER BY created_at DESC
            """, (rs, n) -> new PostView(String.valueOf(rs.getLong("post_id")), "course-" + rs.getLong("course_id"),
                String.valueOf(rs.getLong("reservation_id")), rs.getString("title"), rs.getString("content"),
                rs.getString("status"), rs.getString("course_snapshot")), memberId);
    }

    public PlaceDetails placeDetails(Long placeId, String name, String region) {
        String select = """
            SELECT p.place_id,p.name,p.address,p.description,p.phone,p.opening_hours,
                (SELECT image.image_url FROM place_image image WHERE image.place_id=p.place_id
                 AND NULLIF(TRIM(image.image_url),'') IS NOT NULL
                 ORDER BY CASE WHEN image.image_type='MAIN' THEN 0 ELSE 1 END,
                          image.sort_order,image.place_image_id LIMIT 1) AS image_url
            FROM place p LEFT JOIN region area ON area.region_id=p.region_id WHERE p.is_active=TRUE
            """;
        List<Object> args = new ArrayList<>();
        if (placeId != null) {
            select += " AND p.place_id=?"; args.add(placeId);
        } else {
            select += " AND REPLACE(REPLACE(p.name,' ',''),'해수욕장','')=REPLACE(REPLACE(?,' ',''),'해수욕장','')";
            args.add(name);
            if (!region.isBlank()) {
                select += """
                     AND (LOCATE(?,p.address)=1 OR LOCATE(CONCAT(' ',?,' '),p.address)>0
                       OR LOCATE(CONCAT(' ',?,'시 '),p.address)>0 OR LOCATE(CONCAT(' ',?,'군 '),p.address)>0
                       OR area.region_name=? OR area.sigungu_name=? OR LOCATE(?,area.sido_name)=1
                       OR (? IN ('광주','광주광역시') AND p.address REGEXP
                           '^전남광주통합특별시 (동구|서구|남구|북구|광산구) '))
                    """;
                for (int i=0;i<8;i++) args.add(region);
            }
        }
        var matches = jdbc.query(select + " ORDER BY p.place_id LIMIT 2", (rs,n) -> new PlaceDetails(
                rs.getLong("place_id"),rs.getString("name"),rs.getString("address"),rs.getString("description"),
                rs.getString("image_url"),rs.getString("phone"),rs.getString("opening_hours")),args.toArray());
        // 이름만 같은 다른 장소를 임의로 보여주지 않는다.
        return matches.size()==1 ? matches.getFirst() : null;
    }

    private List<Day> readDays(String json) {
        return List.of(mapper.readValue(json, Day[].class));
    }
}
