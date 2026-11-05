package com.smarttraveler.admin.service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.YearMonth;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HumanManagementService {
    private final JdbcTemplate jdbc;
    public HumanManagementService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public record CourseCard(Long postId, String title, String region, long likeCount, String authorNickname) {}
    public record Exposure(String slotKey, Long postId, String scope, String region, String title, long likeCount, String authorNickname) {}
    public record CommentCard(Long commentId, Long postId, String postTitle, Long authorId,
        String authorNickname, String content, String region, java.time.LocalDateTime createdAt,
        java.time.LocalDateTime deletedAt, boolean deleted) {}
    public record PopularCard(Long postId, String title, String region, long likeCount,
        String authorNickname, boolean selected, Integer rank) {}

    private static CourseCard card(ResultSet r) throws SQLException {
        return new CourseCard(r.getLong("post_id"), r.getString("title"), r.getString("region"),
            r.getLong("like_count"), r.getString("nickname"));
    }
    private static final String CARD_SQL = """
        SELECT p.post_id, p.title, c.destination AS region, m.nickname,
          (SELECT COUNT(*) FROM human_course_like l WHERE l.post_id=p.post_id) AS like_count
        FROM human_course_post p JOIN course c ON c.id=p.course_id
        JOIN members m ON m.member_id=p.author_id
        WHERE p.status='VISIBLE' AND p.deleted_at IS NULL
        """;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void migrateOldFeatured() {
        jdbc.update("""
            INSERT IGNORE INTO admin_human_exposure(post_id,scope,region,created_at,created_by)
            SELECT f.post_id,'MAIN','',COALESCE(f.updated_at,NOW()),NULL
            FROM admin_human_featured f JOIN human_course_post p ON p.post_id=f.post_id
            WHERE f.featured=TRUE AND p.status='VISIBLE' AND p.deleted_at IS NULL
            """);
        jdbc.update("UPDATE admin_human_featured SET featured=FALSE WHERE featured=TRUE");
    }

    @Transactional(readOnly = true)
    public List<CommentCard> comments(String region, java.time.LocalDate from, java.time.LocalDate to,
                                      String query, Long postId) {
        return jdbc.query("""
            SELECT co.comment_id, co.post_id, p.title AS post_title, co.author_id,
              m.nickname, m.login_id, co.content, c.destination AS region, co.created_at, co.deleted_at
            FROM human_course_comment co JOIN human_course_post p ON p.post_id=co.post_id
            JOIN course c ON c.id=p.course_id JOIN members m ON m.member_id=co.author_id
            WHERE (? IS NULL OR c.destination LIKE CONCAT('%',?,'%'))
              AND (? IS NULL OR co.created_at >= ?)
              AND (? IS NULL OR co.created_at < DATE_ADD(?, INTERVAL 1 DAY))
              AND (? IS NULL OR co.content LIKE CONCAT('%',?,'%') OR m.nickname LIKE CONCAT('%',?,'%') OR m.login_id LIKE CONCAT('%',?,'%'))
              AND (? IS NULL OR co.post_id=?)
            ORDER BY co.created_at DESC, co.comment_id DESC
            """, (r, n) -> new CommentCard(r.getLong("comment_id"), r.getLong("post_id"),
                r.getString("post_title"), r.getLong("author_id"), r.getString("nickname"),
                r.getString("content"), r.getString("region"), r.getTimestamp("created_at").toLocalDateTime(),
                r.getTimestamp("deleted_at") == null ? null : r.getTimestamp("deleted_at").toLocalDateTime(),
                r.getTimestamp("deleted_at") != null),
            region, region, from, from, to, to, query, query, query, query, postId, postId);
    }

    @Transactional(readOnly = true)
    public List<Exposure> exposures(String scope, String region) {
        if (!"MAIN".equals(scope) && !"REGION".equals(scope)) throw new IllegalArgumentException("scope는 MAIN 또는 REGION이어야 합니다.");
        String slotRegion = "MAIN".equals(scope) ? "" : region == null ? "" : region.trim();
        return jdbc.query("""
            SELECT e.post_id, e.scope, e.region, p.title, m.nickname,
              (SELECT COUNT(*) FROM human_course_like l WHERE l.post_id=e.post_id) AS like_count
            FROM admin_human_exposure e JOIN human_course_post p ON p.post_id=e.post_id
            JOIN members m ON m.member_id=p.author_id
            WHERE e.scope=? AND (?='' OR e.region=?) ORDER BY e.created_at DESC, e.id DESC
            """, (r,n) -> new Exposure(r.getLong("post_id") + ":" + r.getString("scope") + ":" + r.getString("region"),
                r.getLong("post_id"), r.getString("scope"), r.getString("region"),
                r.getString("title"), r.getLong("like_count"), r.getString("nickname")), scope,slotRegion,slotRegion);
    }

    @Transactional(readOnly = true)
    public List<CourseCard> publicExposures(String scope, String region) {
        validateScope(scope, region);
        return jdbc.query(CARD_SQL + """
            AND EXISTS (SELECT 1 FROM admin_human_exposure e WHERE e.post_id=p.post_id AND e.scope=? AND e.region=?)
            ORDER BY p.created_at DESC, p.post_id DESC
            """, (r,n) -> card(r), scope, normalizedRegion(scope,region));
    }

    @Transactional
    public void setExposure(Long postId, String scope, String region, boolean selected, String login) {
        validateScope(scope, region);
        String slotRegion = normalizedRegion(scope,region);
        if (selected) {
            var rows = jdbc.queryForList("""
                SELECT p.status, p.deleted_at, c.destination FROM human_course_post p
                JOIN course c ON c.id=p.course_id WHERE p.post_id=?
                """, postId);
            if (rows.isEmpty()) throw new IllegalArgumentException("코스가 없습니다.");
            var row=rows.get(0);
            if (!"VISIBLE".equals(row.get("status")) || row.get("deleted_at") != null)
                throw new IllegalArgumentException("게시 중인 코스만 노출할 수 있습니다.");
            if ("REGION".equals(scope) && !String.valueOf(row.get("destination")).contains(slotRegion))
                throw new IllegalArgumentException("코스 목적지와 노출 지역이 다릅니다.");
            jdbc.update("""
                INSERT IGNORE INTO admin_human_exposure(post_id,scope,region,created_at,created_by)
                SELECT ?,?,?,NOW(),member_id FROM members WHERE login_id=? AND role_code='ADMIN'
                """, postId, scope, slotRegion, login);
        } else {
            jdbc.update("DELETE FROM admin_human_exposure WHERE post_id=? AND scope=? AND region=?", postId,scope,slotRegion);
        }
        action(postId,login,selected ? "EXPOSE" : "UNEXPOSE",scope + (slotRegion.isEmpty() ? "" : ":" + slotRegion));
    }

    @Transactional
    public void clearSelections(Long postId) {
        jdbc.update("DELETE FROM admin_human_exposure WHERE post_id=?",postId);
        jdbc.update("DELETE FROM admin_human_monthly_pick WHERE post_id=?",postId);
    }

    @Transactional(readOnly = true)
    public List<PopularCard> popularity(String month) {
        YearMonth ym=parseMonth(month);
        return jdbc.query("""
            SELECT p.post_id,p.title,c.destination AS region,m.nickname,
              COALESCE(l.like_count,0) AS like_count,pk.pick_rank
            FROM human_course_post p JOIN course c ON c.id=p.course_id
            JOIN members m ON m.member_id=p.author_id
            LEFT JOIN (SELECT post_id,COUNT(*) AS like_count FROM human_course_like
              WHERE created_at>=? AND created_at<? GROUP BY post_id) l ON l.post_id=p.post_id
            LEFT JOIN admin_human_monthly_pick pk ON pk.post_id=p.post_id AND pk.pick_month=?
            WHERE p.status='VISIBLE' AND p.deleted_at IS NULL
            ORDER BY like_count DESC,p.created_at DESC,p.post_id DESC LIMIT 100
            """, (r,n) -> new PopularCard(r.getLong("post_id"),r.getString("title"),r.getString("region"),
                r.getLong("like_count"),r.getString("nickname"),r.getObject("pick_rank") != null,
                (Integer) r.getObject("pick_rank")), ym.atDay(1).atStartOfDay(), ym.plusMonths(1).atDay(1).atStartOfDay(),month);
    }

    @Transactional(readOnly = true)
    public List<CourseCard> monthlyPicks(String month) {
        YearMonth ym=parseMonth(month);
        return jdbc.query("""
            SELECT p.post_id,p.title,c.destination AS region,m.nickname,
              (SELECT COUNT(*) FROM human_course_like l WHERE l.post_id=p.post_id
                AND l.created_at>=? AND l.created_at<?) AS like_count
            FROM admin_human_monthly_pick pk JOIN human_course_post p ON p.post_id=pk.post_id
            JOIN course c ON c.id=p.course_id JOIN members m ON m.member_id=p.author_id
            WHERE pk.pick_month=? AND p.status='VISIBLE' AND p.deleted_at IS NULL
            ORDER BY like_count DESC,p.created_at DESC,p.post_id DESC
            """, (r,n) -> card(r), ym.atDay(1).atStartOfDay(),ym.plusMonths(1).atDay(1).atStartOfDay(),month);
    }

    @Transactional
    public void setMonthlyPick(String month, Long postId, boolean selected, String login) {
        parseMonth(month);
        if (selected) {
            var rows=jdbc.queryForList("SELECT status,deleted_at FROM human_course_post WHERE post_id=?",postId);
            if (rows.isEmpty()) throw new IllegalArgumentException("코스가 없습니다.");
            if (!"VISIBLE".equals(rows.get(0).get("status")) || rows.get(0).get("deleted_at") != null)
                throw new IllegalArgumentException("게시 중인 코스만 선정할 수 있습니다.");
            jdbc.update("""
                INSERT IGNORE INTO admin_human_monthly_pick(pick_month,post_id,pick_rank,created_by,created_at)
                SELECT ?,?,(SELECT COALESCE(MAX(pick_rank),0)+1 FROM admin_human_monthly_pick x WHERE x.pick_month=?),member_id,NOW()
                FROM members WHERE login_id=? AND role_code='ADMIN'
                """,month,postId,month,login);
        } else {
            jdbc.update("DELETE FROM admin_human_monthly_pick WHERE pick_month=? AND post_id=?",month,postId);
        }
        action(postId,login,selected ? "MONTHLY_PICK" : "MONTHLY_UNPICK",month);
    }

    private void action(Long postId,String login,String type,String note) {
        jdbc.update("""
            INSERT INTO admin_human_action_history(course_id,admin_id,action,note,created_at)
            SELECT ?,member_id,?,?,NOW() FROM members WHERE login_id=? AND role_code='ADMIN'
            """,postId,type,note,login);
    }
    private static YearMonth parseMonth(String month) {
        try { return YearMonth.parse(month); }
        catch (RuntimeException ex) { throw new IllegalArgumentException("month는 YYYY-MM 형식이어야 합니다."); }
    }
    private static void validateScope(String scope,String region) {
        if (!"MAIN".equals(scope) && !"REGION".equals(scope)) throw new IllegalArgumentException("scope는 MAIN 또는 REGION이어야 합니다.");
        if ("REGION".equals(scope) && (region==null || region.isBlank())) throw new IllegalArgumentException("REGION에는 region이 필요합니다.");
        if ("MAIN".equals(scope) && region!=null && !region.isBlank()) throw new IllegalArgumentException("MAIN에는 region을 지정할 수 없습니다.");
    }
    private static String normalizedRegion(String scope,String region) {
        return "MAIN".equals(scope) ? "" : region.trim();
    }
}
