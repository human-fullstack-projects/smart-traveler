package com.smarttraveler.human_course;

import com.smarttraveler.admin.service.HumanManagementService;
import com.smarttraveler.auth.repository.MemberRepository;
import com.smarttraveler.human_course.HumanCourseDtos.*;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

@Service
public class HumanCourseService {
    private static final ZoneId KOREA = ZoneId.of("Asia/Seoul");
    private static final Set<String> CATEGORIES = Set.of("맛집", "문화", "힐링", "액티비티");
    private static final Set<String> COMPANIONS = Set.of("혼자", "친구", "연인", "가족");
    private static final Set<String> REPORT_REASONS = Set.of("광고·홍보", "욕설·비방", "부적절한 내용", "기타");
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final MemberRepository members;
    private final HumanCoursePhotoStore photos;
    private final HumanManagementService management;

    public HumanCourseService(JdbcTemplate jdbc, ObjectMapper mapper, MemberRepository members,
                              HumanCoursePhotoStore photos, HumanManagementService management) {
        this.jdbc = jdbc; this.mapper = mapper; this.members = members;
        this.photos = photos; this.management = management;
    }

    public record Snapshot(String region, String startDate, String endDate, int version, List<Day> plan) {}

    private long member(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken)
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        var account = members.findByLoginId(auth.getName()).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if (!"ACTIVE".equals(account.getStatus()) || account.getDeletedAt() != null)
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        return account.getMemberId();
    }

    private long viewer(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) return -1;
        return member(auth);
    }

    @Transactional(readOnly = true)
    public List<Post> list(Authentication auth) {
        long viewer = viewer(auth);
        return jdbc.queryForList("SELECT post_id FROM human_course_post WHERE status='VISIBLE' AND deleted_at IS NULL ORDER BY created_at DESC, post_id DESC")
            .stream().map(row -> post(((Number) row.get("post_id")).longValue(), viewer, false)).toList();
    }
        @Transactional(readOnly = true)
    public List<Post> homeReviews(Authentication auth) {
        long viewerId = viewer(auth);
        if (viewerId < 0) {
            return List.of();
        }

        List<Long> postIds = jdbc.queryForList("""
            SELECT p.post_id
            FROM human_course_post p
            JOIN (
                SELECT author_tag.member_no, COUNT(*) AS shared_likes
                FROM member_preference_tag viewer_tag
                JOIN member_preference_tag author_tag
                  ON author_tag.tag_id = viewer_tag.tag_id
                WHERE viewer_tag.member_no = ?
                  AND viewer_tag.polarity = 'like'
                  AND author_tag.polarity = 'like'
                GROUP BY author_tag.member_no
            ) preference ON preference.member_no = p.author_id
            WHERE p.status = 'VISIBLE'
              AND p.deleted_at IS NULL
              AND p.author_id <> ?
            ORDER BY preference.shared_likes DESC, p.created_at DESC, p.post_id DESC
            LIMIT 3
            """, Long.class, viewerId, viewerId);

        return postIds.stream()
            .map(postId -> post(postId, viewerId, false))
            .toList();
    }
    public Post detail(long id, Authentication auth) { return post(id, viewer(auth), true); }

    private Post post(long id, long viewer, boolean allowBlinded) {
        var rows = jdbc.queryForList("""
            SELECT p.*, c.destination AS region, c.start_date, c.end_date, c.lock_version,
                   COALESCE(NULLIF(m.nickname,''),m.login_id) AS author,
                   r.checkin_date, r.checkout_date,
                   (SELECT COUNT(*) FROM human_course_like WHERE post_id=p.post_id) AS likes,
                   EXISTS(SELECT 1 FROM human_course_like WHERE post_id=p.post_id AND member_id=?) AS liked,
                   EXISTS(SELECT 1 FROM human_course_report WHERE post_id=p.post_id AND reporter_id=?) AS reported
            FROM human_course_post p JOIN course c ON c.id=p.course_id
            JOIN members m ON m.member_id=p.author_id JOIN room_reservation r ON r.reservation_id=p.reservation_id
            WHERE p.post_id=? AND p.deleted_at IS NULL AND p.status IN ('VISIBLE','BLINDED')
            """, viewer, viewer, id);
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "코스를 찾을 수 없습니다.");
        Map<String, Object> row = rows.getFirst();
        boolean mine = number(row, "author_id") == viewer;
        boolean blinded = "BLINDED".equals(row.get("status"));
        if (blinded && !allowBlinded) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        Snapshot snapshot = snapshot(row);
        List<String> urls = blinded ? List.of() : jdbc.queryForList("SELECT image_url FROM human_course_photo WHERE post_id=? ORDER BY sort_order, photo_id", String.class, id);
        List<Photo> photoAssets = new ArrayList<>();
        for (int index = 0; index < urls.size(); index++) photoAssets.add(new Photo("사진 " + (index + 1), urls.get(index)));
        List<Comment> comments = blinded ? List.of() : jdbc.query("""
            SELECT co.comment_id, co.content, co.author_id, COALESCE(NULLIF(m.nickname,''),m.login_id) AS author
            FROM human_course_comment co JOIN members m ON m.member_id=co.author_id
            WHERE co.post_id=? AND co.deleted_at IS NULL ORDER BY co.created_at, co.comment_id
            """, (rs, n) -> new Comment(String.valueOf(rs.getLong("comment_id")), rs.getString("author"),
                rs.getString("content"), rs.getLong("author_id") == viewer), id);
        String body = blinded ? "" : string(row, "content");
        return new Post(id, string(row, "title"), string(row, "author"), string(row, "region"),
            string(row, "category") == null ? "미분류" : string(row, "category"), string(row, "companion_type"),
            string(row, "summary") == null ? body.substring(0, Math.min(150, body.length())) : string(row, "summary"),
            body, photoAssets.stream().map(Photo::name).toList(), photoAssets,
            blinded ? List.of() : snapshot.plan(), number(row, "likes"), truthy(row.get("liked")),
            truthy(row.get("reported")), comments, row.get("created_at").toString().substring(0, 10),
            blinded ? "BLINDED" : "PUBLIC", snapshot.version(),
            row.get("checkin_date") + " ~ " + row.get("checkout_date"), mine, String.valueOf(row.get("reservation_id")));
    }

    @Transactional(readOnly = true)
    public List<Reservation> reservations(Authentication auth) {
        long owner = member(auth);
        return jdbc.queryForList("""
            SELECT r.reservation_id, r.status, r.checkin_date, r.checkout_date, r.course_id,
                   l.name AS stay_name, l.checkout_time, c.destination, c.status AS course_status,
                   c.lock_version, c.member_no, c.is_active, c.deleted_at AS course_deleted,
                   (SELECT post_id FROM human_course_post p WHERE p.reservation_id=r.reservation_id AND p.deleted_at IS NULL LIMIT 1) AS existing_post_id,
                   EXISTS(SELECT 1 FROM human_course_post p WHERE p.reservation_id=r.reservation_id) AS used_post
            FROM room_reservation r JOIN room ro ON ro.room_id=r.room_id
            JOIN lodging l ON l.lodging_id=ro.lodging_id
            LEFT JOIN course c ON c.id=r.course_id WHERE r.member_id=?
            ORDER BY r.checkout_date DESC, r.reservation_id DESC
            """, owner).stream().map(row -> reservation(row, owner)).toList();
    }

    private Reservation reservation(Map<String, Object> row, long owner) {
        Long courseId = row.get("course_id") == null ? null : number(row, "course_id");
        List<Day> plan = courseId == null ? List.of() : plan(courseId);
        boolean finalCourse = courseId != null && row.get("member_no") != null && number(row, "member_no") == owner
            && "done".equals(row.get("course_status")) && truthy(row.get("is_active"))
            && row.get("course_deleted") == null && plan.stream().anyMatch(day -> !day.stops().isEmpty());
        LocalDate checkout = date(row.get("checkout_date"));
        LocalTime checkoutTime = row.get("checkout_time") == null ? LocalTime.of(11, 0)
            : row.get("checkout_time") instanceof LocalTime time ? time
            : row.get("checkout_time") instanceof java.sql.Time time ? time.toLocalTime()
            : LocalTime.parse(row.get("checkout_time").toString());
        LocalDateTime checkoutAt = checkout.atTime(checkoutTime);
        String error = eligibility(string(row, "status"), checkoutAt, finalCourse, truthy(row.get("used_post")));
        return new Reservation(String.valueOf(row.get("reservation_id")), string(row, "stay_name"),
            string(row, "destination"), row.get("checkin_date") + " ~ " + row.get("checkout_date"),
            string(row, "status"), checkoutAt.toString(), finalCourse,
            courseId == null ? 0 : Math.max(1, (int) number(row, "lock_version")), plan,
            row.get("existing_post_id") == null ? null : number(row, "existing_post_id"), error);
    }

    static String eligibility(String status, LocalDateTime checkoutAt, boolean finalCourse, boolean used) {
        if (used) return "이 예약은 이미 작성한 코스가 있습니다. 삭제한 글도 다시 작성할 수 없습니다.";
        if (!"COMPLETED".equals(status)) return "숙박 완료 예약만 작성할 수 있습니다.";
        if (!checkoutAt.isBefore(LocalDateTime.now(KOREA))) return "체크아웃 시각이 지나야 작성할 수 있습니다.";
        if (!finalCourse) return "저장된 최종 코스가 없습니다.";
        return "";
    }

    private List<Day> plan(long courseId) {
        return jdbc.query("SELECT id, day_no FROM course_day WHERE course_id=? ORDER BY day_no", (rs, n) ->
            new Day(rs.getInt("day_no"), jdbc.queryForList("""
                SELECT CONCAT(TIME_FORMAT(start_time,'%H:%i'),' ',place_name)
                FROM activity WHERE course_day_id=? ORDER BY seq
                """, String.class, rs.getLong("id"))), courseId);
    }

    @Transactional
    public IdResult create(Authentication auth, long reservationId, PostInput input) {
        long owner = member(auth);
        validate(input);
        var rows = jdbc.queryForList("SELECT reservation_id FROM room_reservation WHERE reservation_id=? AND member_id=? FOR UPDATE", reservationId, owner);
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "예약을 찾을 수 없습니다.");
        Reservation reservation = reservations(auth).stream().filter(r -> r.id().equals(String.valueOf(reservationId)))
            .findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!reservation.eligibilityError().isEmpty()) throw new ResponseStatusException(HttpStatus.CONFLICT, reservation.eligibilityError());
        long courseId = jdbc.queryForObject("SELECT course_id FROM room_reservation WHERE reservation_id=?", Long.class, reservationId);
        Snapshot snapshot = new Snapshot(reservation.region(), reservation.dates().substring(0, 10),
            reservation.dates().substring(reservation.dates().length() - 10), reservation.courseVersion(), reservation.plan());
        var key = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO human_course_post(reservation_id,author_id,course_id,title,category,summary,companion_type,
                  content,status,course_snapshot,created_at,updated_at)
                VALUES (?,?,?,?,?,?,?,?,'VISIBLE',?,NOW(),NOW())
                """, Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, reservationId); statement.setLong(2, owner); statement.setLong(3, courseId);
            statement.setString(4, input.title().trim()); statement.setString(5, input.category());
            statement.setString(6, input.summary().trim()); statement.setString(7, input.companion());
            statement.setString(8, input.body().trim()); statement.setString(9, mapper.writeValueAsString(snapshot));
            return statement;
        }, key);
        long id = key.getKey().longValue();
        savePhotos(id, input.photoAssets());
        return new IdResult(id);
    }

    @Transactional
    public IdResult edit(Authentication auth, long id, PostInput input) {
        long owner = member(auth);
        validate(input);
        var rows = jdbc.queryForList("SELECT author_id,status FROM human_course_post WHERE post_id=? AND deleted_at IS NULL FOR UPDATE", id);
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        if (number(rows.getFirst(), "author_id") != owner) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        jdbc.update("UPDATE human_course_post SET title=?,category=?,summary=?,companion_type=?,content=?,updated_at=NOW() WHERE post_id=?",
            input.title().trim(), input.category(), input.summary().trim(), input.companion(), input.body().trim(), id);
        if (input.replacePhotos()) {
            jdbc.update("DELETE FROM human_course_photo WHERE post_id=?", id);
            savePhotos(id, input.photoAssets());
        }
        return new IdResult(id);
    }

    private void savePhotos(long id, List<Photo> assets) {
        for (int i = 0; i < assets.size(); i++) {
            String url = photos.save(assets.get(i));
            jdbc.update("INSERT INTO human_course_photo(post_id,image_url,sort_order,created_at) VALUES (?,?,?,NOW())", id, url, i + 1);
        }
    }

    @Transactional
    public void delete(Authentication auth, long id) {
        long owner = member(auth);
        int changed = jdbc.update("UPDATE human_course_post SET status='DELETED',deleted_at=NOW(),updated_at=NOW() WHERE post_id=? AND author_id=? AND deleted_at IS NULL", id, owner);
        if (changed == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "삭제할 내 글을 찾을 수 없습니다.");
        management.clearSelections(id);
        jdbc.update("UPDATE admin_human_featured SET featured=FALSE WHERE post_id=?", id);
    }

    @Transactional
    public Post like(Authentication auth, long id) {
        long owner = member(auth);
        requireVisible(id);
        int removed = jdbc.update("DELETE FROM human_course_like WHERE post_id=? AND member_id=?", id, owner);
        if (removed == 0) jdbc.update("INSERT INTO human_course_like(post_id,member_id,created_at) VALUES (?,?,NOW())", id, owner);
        return post(id, owner, true);
    }

    @Transactional
    public Post comment(Authentication auth, long id, CommentInput input) {
        long owner = member(auth);
        requireVisible(id);
        String text = input == null || input.text() == null ? "" : input.text().trim();
        if (text.isEmpty() || text.length() > 300) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "댓글은 1~300자로 입력해주세요.");
        jdbc.update("INSERT INTO human_course_comment(post_id,author_id,content,created_at,updated_at) VALUES (?,?,?,NOW(),NOW())", id, owner, text);
        return post(id, owner, true);
    }

    @Transactional
    public Post deleteComment(Authentication auth, long id, long commentId) {
        long owner = member(auth);
        requireVisible(id);
        int changed = jdbc.update("UPDATE human_course_comment SET deleted_at=NOW() WHERE comment_id=? AND post_id=? AND author_id=? AND deleted_at IS NULL", commentId, id, owner);
        if (changed == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "삭제할 내 댓글을 찾을 수 없습니다.");
        return post(id, owner, true);
    }

    @Transactional
    public Post report(Authentication auth, long id, ReportInput input) {
        long owner = member(auth);
        requireVisible(id);
        if (jdbc.queryForObject("SELECT author_id FROM human_course_post WHERE post_id=?", Long.class, id) == owner)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "본인 글은 신고할 수 없습니다.");
        if (input == null || !REPORT_REASONS.contains(input.reason()) || input.details() != null && input.details().length() > 300)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "신고 사유를 확인해주세요.");
        if (!jdbc.queryForList("SELECT report_id FROM human_course_report WHERE post_id=? AND reporter_id=?", id, owner).isEmpty())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 신고한 글입니다.");
        jdbc.update("INSERT INTO human_course_report(post_id,reporter_id,reason,status,created_at) VALUES (?,?,?,'PENDING',NOW())",
            id, owner, input.reason() + (input.details() == null || input.details().isBlank() ? "" : " / " + input.details().trim()));
        return post(id, owner, true);
    }

    private void requireVisible(long id) {
        if (jdbc.queryForList("SELECT post_id FROM human_course_post WHERE post_id=? AND status='VISIBLE' AND deleted_at IS NULL", id).isEmpty())
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "공개된 코스를 찾을 수 없습니다.");
    }

    private static void validate(PostInput input) {
        if (input == null || blank(input.title(), 100) || blank(input.summary(), 150) || blank(input.body(), 2000)
            || !CATEGORIES.contains(input.category()) || !COMPANIONS.contains(input.companion())
            || input.photoAssets() == null || input.photoAssets().size() > 5)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "제목·동행·여행 이야기·사진 입력을 확인해주세요.");
    }

    private static boolean blank(String value, int max) { return value == null || value.isBlank() || value.trim().length() > max; }
    private static long number(Map<String, Object> row, String key) { return ((Number) row.get(key)).longValue(); }
    private static boolean truthy(Object value) { return Boolean.TRUE.equals(value) || value instanceof Number number && number.intValue() != 0; }
    private static LocalDate date(Object value) { return value instanceof LocalDate date ? date
        : value instanceof java.sql.Date date ? date.toLocalDate() : LocalDate.parse(value.toString()); }
    private static String string(Map<String, Object> row, String key) { return row.get(key) == null ? null : row.get(key).toString(); }
    private Snapshot snapshot(Map<String, Object> row) {
        try {
            Snapshot value = mapper.readValue(string(row, "course_snapshot"), Snapshot.class);
            if (value != null && value.plan() != null && value.version() > 0) return value;
        }
        catch (RuntimeException error) { return new Snapshot(string(row, "region"), row.get("start_date").toString(),
            row.get("end_date").toString(), Math.max(1, (int) number(row, "lock_version")), plan(number(row, "course_id"))); }
        return new Snapshot(string(row, "region"), row.get("start_date").toString(),
            row.get("end_date").toString(), Math.max(1, (int) number(row, "lock_version")), plan(number(row, "course_id")));
    }

    @Transactional
    public int completeStays() {
        return jdbc.update("""
            UPDATE room_reservation r JOIN room ro ON ro.room_id=r.room_id
            JOIN lodging l ON l.lodging_id=ro.lodging_id
            SET r.status='COMPLETED',r.version=r.version+1,r.updated_at=?
            WHERE r.status='CONFIRMED' AND TIMESTAMP(r.checkout_date,COALESCE(l.checkout_time,'11:00:00')) < ?
            """, LocalDateTime.now(KOREA), LocalDateTime.now(KOREA));
    }

    @Transactional(readOnly = true)
    public void requirePhotoReadable(String name, Authentication auth) {
        long viewer = viewer(auth);
        String url = "/api/human-courses/photos/" + name;
        if (jdbc.queryForList("""
            SELECT ph.photo_id FROM human_course_photo ph JOIN human_course_post p ON p.post_id=ph.post_id
            WHERE ph.image_url=? AND p.deleted_at IS NULL AND (p.status='VISIBLE' OR (p.status='BLINDED' AND p.author_id=?))
            """, url, viewer).isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
}
