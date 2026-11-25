package com.smarttraveler.mypage.repository;

import static com.smarttraveler.domain.entity.RoomReservation.PAYMENT_TIMEOUT_REASON;

import com.smarttraveler.mypage.dto.MyPageDtos.*;
import com.smarttraveler.reservation.dto.ReservationItinerary;
import com.smarttraveler.tour.service.TourPhotoService.Target;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.core.JacksonException;
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

    public String humanDraftClientId(long memberId, long postId) {
        String base = "human-course-route-" + postId;
        record SavedDraft(long id, String clientId) {}
        var latest = jdbc.query("""
            SELECT id, client_id FROM mypage_course_draft
            WHERE member_id=? AND (client_id=? OR client_id LIKE CONCAT(?, '-%'))
            ORDER BY id DESC LIMIT 1
            """, (rs, row) -> new SavedDraft(rs.getLong("id"), rs.getString("client_id")), memberId, base, base);
        if (latest.isEmpty()) return base;
        var draft = latest.getFirst();
        return bookedCourseIds(memberId).contains("draft-" + draft.id()) ? base + "-" + draft.id() : draft.clientId();
    }

    public List<CourseView> courses(long memberId) {
        Set<String> bookedIds = bookedCourseIds(memberId);
        Map<String, Optional<String>> regionalPhotos = new HashMap<>();
        List<CourseView> result = new ArrayList<>(jdbc.query("""
            SELECT id, title, region, start_date, end_date, days_json
            FROM mypage_course_draft WHERE member_id=? ORDER BY id DESC
            """, (rs, n) -> {
                String region = rs.getString("region");
                List<Day> days = readDays(rs.getString("days_json"));
                String photo = draftPhoto(days, region);
                String id = "draft-" + rs.getLong("id");
                return new CourseView(id, rs.getString("title"), region,
                    rs.getObject("start_date", LocalDate.class), rs.getObject("end_date", LocalDate.class),
                    bookedIds.contains(id) ? "BOOKED" : "DRAFT", days,
                    photo != null ? photo : regionalPhoto(region, regionalPhotos));
            }, memberId));
        result.addAll(jdbc.query("""
            SELECT c.id, COALESCE(c.title, c.destination) AS title, c.destination, c.start_date, c.end_date,
                   c.region_id, c.adults, c.children, l.lodging_id, l.name AS lodging_name, l.address AS lodging_address,
                   CASE WHEN JSON_EXTRACT(c.condition_meta, '$.chat_message_id') IS NOT NULL
                        THEN 'DRAFT' ELSE c.status END AS status,
                   (SELECT image.image_url FROM course_day d
                    JOIN activity a ON a.course_day_id=d.id
                    JOIN place_image image ON image.place_id=a.place_id
                    WHERE d.course_id=c.id AND NULLIF(TRIM(image.image_url),'') IS NOT NULL
                    ORDER BY d.day_no, a.seq, a.id, image.sort_order, image.place_image_id LIMIT 1) AS itinerary_photo
            FROM course c
            LEFT JOIN lodging l ON l.lodging_id = COALESCE(
                JSON_VALUE(c.condition_meta, '$.lodging_id'),
                CASE WHEN JSON_EXTRACT(c.condition_meta, '$.chat_message_id') IS NOT NULL
                    THEN JSON_VALUE(c.preferences_snapshot, '$.lodgingId') END)
            WHERE c.member_no=? AND c.deleted_at IS NULL AND c.is_active=TRUE ORDER BY c.id DESC
            """, (rs, n) -> {
                String region = rs.getString("destination");
                String photo = rs.getString("itinerary_photo");
                String id = "course-" + rs.getLong("id");
                Long lodgingId = rs.getObject("lodging_id", Long.class);
                CourseStay stay = lodgingId == null ? null
                    : new CourseStay(lodgingId, rs.getString("lodging_name"), rs.getString("lodging_address"));
                return new CourseView(id, rs.getString("title"), region,
                    rs.getObject("start_date", LocalDate.class), rs.getObject("end_date", LocalDate.class),
                    bookedIds.contains(id) ? "BOOKED" : rs.getString("status"), List.of(),
                    photo != null ? photo : regionalPhoto(region, regionalPhotos),
                    rs.getObject("region_id", Long.class), rs.getObject("adults", Integer.class),
                    rs.getObject("children", Integer.class), stay);
            }, memberId));
        return result;
    }

    private Set<String> bookedCourseIds(long memberId) {
        return new HashSet<>(jdbc.queryForList("""
            SELECT DISTINCT COALESCE(CONCAT('course-', r.course_id),
                JSON_UNQUOTE(JSON_EXTRACT(IF(JSON_VALID(r.itinerary_snapshot), r.itinerary_snapshot, NULL), '$.course.id')))
            FROM room_reservation r
            WHERE r.member_id=? AND (r.course_id IS NOT NULL OR r.itinerary_snapshot IS NOT NULL)
              AND (r.status IN ('CONFIRMED', 'COMPLETED', 'REFUNDING', 'REFUND_FAILED')
                OR (r.status='CANCELLED' AND EXISTS (SELECT 1 FROM room_payment p
                    WHERE p.reservation_id=r.reservation_id AND p.approved_at IS NOT NULL)))
            """, String.class, memberId));
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
            return new CourseView(c.id(), c.title(), c.region(), c.startDate(), c.endDate(), c.state(), days, c.coverImageUrl(),
                    c.regionId(), c.adults(), c.children(), c.stay());
        }).orElse(null);
    }

    public List<BookingView> bookings(long memberId) {
        Map<String, Optional<String>> regionalPhotos = new HashMap<>();
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
            WHERE r.member_id=? AND r.status<>'PENDING_PAYMENT'
              AND NOT (r.status='CANCELLED' AND r.cancel_reason <=> ?)
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
                   display_stay.latitude AS stay_latitude, display_stay.longitude AS stay_longitude,
                   COALESCE(match_region, stay_region.sigungu_name, stay_region.region_name) AS cover_region,
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
            LEFT JOIN region stay_region ON stay_region.region_id=display_stay.region_id
            ORDER BY matched.created_at DESC, matched.reservation_id DESC
            """, (rs, n) -> {
                var itinerary = readItinerary(rs.getString("itinerary_snapshot"));
                String courseId = itinerary != null && itinerary.course() != null
                        ? itinerary.course().id() : rs.getString("owned_course_id");
                Long lodgingId = rs.getObject("display_lodging_id", Long.class);
                var stay = lodgingId == null ? null : new StayInfo(lodgingId, rs.getString("stay_address"),
                        rs.getString("stay_phone"), rs.getString("stay_description"), rs.getString("stay_image_url"),
                        rs.getString("stay_checkin_time"), rs.getString("stay_checkout_time"),
                        !lodgingId.equals(rs.getObject("booked_lodging_id", Long.class)),
                        rs.getObject("stay_latitude", Double.class), rs.getObject("stay_longitude", Double.class));
                String cover = courseId == null ? regionalPhoto(rs.getString("cover_region"), regionalPhotos) : null;
                return new BookingView(String.valueOf(rs.getLong("reservation_id")), rs.getString("reservation_no"),
                        courseId, rs.getString("stay_name"), rs.getString("room_name"),
                        rs.getObject("checkin_date", LocalDate.class), rs.getObject("checkout_date", LocalDate.class),
                        rs.getInt("guest_count"), rs.getInt("total_price"), rs.getString("status"), stay, cover, itinerary);
            }, memberId, memberId, PAYMENT_TIMEOUT_REASON);
    }

    private ReservationItinerary readItinerary(String snapshot) {
        if (snapshot == null) return null;
        try {
            return mapper.readValue(snapshot, ReservationItinerary.class);
        } catch (JacksonException ignored) {
            return null;
        }
    }

    private String draftPhoto(List<Day> days, String region) {
        // 임시 일정은 방문 순서대로 장소명을 확인하고, 저장된 장소 ID가 있으면 우선 사용한다.
        for (Day day : days) {
            for (String name : day.places()) {
                Long placeId = null;
                if (day.mapPlaces() != null) {
                    for (MapPlace place : day.mapPlaces()) {
                        if (name.equals(place.name()) && place.placeId() != null) {
                            placeId = place.placeId();
                            break;
                        }
                    }
                }
                PlaceDetails place = placeDetails(placeId, name, region == null ? "" : region.trim());
                if (place != null && place.imageUrl() != null) return place.imageUrl();
            }
        }
        return null;
    }

    private String regionalPhoto(String region, Map<String, Optional<String>> photos) {
        if (region == null || region.isBlank()) return null;
        // 같은 지역의 여행이 여러 건이면 대표 장소 사진을 한 번만 조회한다.
        return photos.computeIfAbsent(region.trim(), name -> jdbc.query("""
            WITH target AS (SELECT ? AS name)
            SELECT image.image_url FROM place p
            JOIN region area ON area.region_id=p.region_id
            JOIN place_image image ON image.place_id=p.place_id
            CROSS JOIN target
            WHERE p.is_active=TRUE AND NULLIF(TRIM(image.image_url),'') IS NOT NULL
              AND (area.region_name=target.name OR area.sigungu_name=target.name
                   OR LOCATE(target.name,area.sido_name)=1
                   OR LOCATE(CONCAT(target.name,' '),p.address)=1
                   OR LOCATE(CONCAT(' ',target.name,' '),p.address)>0
                   OR LOCATE(CONCAT(' ',target.name,'시 '),p.address)>0
                   OR LOCATE(CONCAT(' ',target.name,'군 '),p.address)>0
                   OR (target.name IN ('광주','광주광역시') AND p.address REGEXP
                       '^전남광주통합특별시 (동구|서구|남구|북구|광산구) '))
            ORDER BY CASE WHEN p.place_type='ATTRACTION' THEN 0 ELSE 1 END,
                     p.place_id, image.sort_order, image.place_image_id LIMIT 1
            """, (rs, n) -> rs.getString("image_url"), name).stream().findFirst()).orElse(null);
    }

    public List<PostView> posts(long memberId) {
        // 등록 사진이 없으면 일차와 방문 순서대로 사진이 있는 첫 여행지를 찾는다.
        return jdbc.query("""
            SELECT p.post_id, p.course_id, p.reservation_id, p.title, p.content, p.status, p.course_snapshot,
                   COALESCE(
                       (SELECT photo.image_url FROM human_course_photo photo
                        WHERE photo.post_id=p.post_id AND NULLIF(TRIM(photo.image_url),'') IS NOT NULL
                        ORDER BY photo.sort_order, photo.photo_id LIMIT 1),
                       (SELECT image.image_url FROM course_day d
                        JOIN activity a ON a.course_day_id=d.id
                        JOIN place_image image ON image.place_id=a.place_id
                        WHERE d.course_id=p.course_id AND NULLIF(TRIM(image.image_url),'') IS NOT NULL
                        ORDER BY d.day_no, a.seq, a.id, image.sort_order, image.place_image_id LIMIT 1)
                   ) AS cover_image_url
            FROM human_course_post p WHERE p.author_id=? AND p.deleted_at IS NULL ORDER BY p.created_at DESC
            """, (rs, n) -> new PostView(String.valueOf(rs.getLong("post_id")), "course-" + rs.getLong("course_id"),
                String.valueOf(rs.getLong("reservation_id")), rs.getString("title"), rs.getString("content"),
                rs.getString("status"), rs.getString("course_snapshot"), rs.getString("cover_image_url")), memberId);
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

    public Target placePhotoTarget(long id, String address, String region) {
        return jdbc.query("""
            SELECT CASE WHEN source_provider='KTO_TOURAPI' THEN source_place_id END AS content_id,
                   name, COALESCE(NULLIF(TRIM(address), ''), ?) AS address
            FROM place WHERE place_id=? AND is_active=TRUE
            """, (rs, row) -> new Target(rs.getString("content_id"), rs.getString("name"), rs.getString("address"), region, false),
                address, id).stream().findFirst().orElse(null);
    }

    public Target lodgingPhotoTarget(long id) {
        return jdbc.query("""
            SELECT CASE WHEN source_provider='TOURAPI' THEN source_content_id END AS content_id, name, address
            FROM lodging WHERE lodging_id=?
            """, (rs, row) -> new Target(rs.getString("content_id"), rs.getString("name"), rs.getString("address"), "", true),
                id).stream().findFirst().orElse(null);
    }

    private List<Day> readDays(String json) {
        return List.of(mapper.readValue(json, Day[].class));
    }
}
