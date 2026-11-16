package com.smarttraveler.admin.controller;

import com.smarttraveler.admin.repository.*;
import com.smarttraveler.admin.dto.AdminViews;
import com.smarttraveler.admin.dto.AdminViews.HumanCourseView;
import com.smarttraveler.admin.dto.AdminViews.HumanReportView;
import com.smarttraveler.domain.entity.Member;
import com.smarttraveler.domain.entity.MemberSuspensionHistory;
import com.smarttraveler.domain.entity.OwnerProfile;
import com.smarttraveler.domain.entity.RecommendationCriteria;
import com.smarttraveler.admin.service.AdminDataService;
import com.smarttraveler.tour.service.TourSyncService;
import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin")
public class AdminRestController {
    private final AdminDataService data;
    private final TourSyncService tour;
    private final JdbcTemplate jdbc;
    public AdminRestController(AdminDataService data, TourSyncService tour, JdbcTemplate jdbc) {
        this.data=data; this.tour=tour; this.jdbc=jdbc;
    }

    @GetMapping({"", "/dashboard"})
    public Map<String,Object> dashboard() {
        // 「전체 회원」은 회원 관리 목록과 같은 기준(여행자·관리자). 업주는 「업주 회원 관리」에서 따로 셈
        return Map.of("members", data.members().stream().filter(x -> MEMBER_ROLES.contains(x.getRoleCode())).count(), "orders", data.reservations().size(),
            "lodgings", data.lodgings().stream().filter(x -> !"DELETED".equals(x.getStatus())).count(), "humanCourses", data.humanCourses().size(),
            "pendingOwners", data.owners().stream().filter(x -> "PENDING".equals(x.getReviewStatusCode())).count(),
            "openReports", data.humanReports().stream().filter(x -> "PENDING".equals(x.getStatus())).count());
    }

    @GetMapping("/orders")
    public List<Map<String,Object>> orders(@RequestParam(required=false) String status,
        @RequestParam(required=false) String type, @RequestParam(required=false) String query,
        @RequestParam(required=false) Long lodgingId,
        @RequestParam(required=false) LocalDate from, @RequestParam(required=false) LocalDate to) {
        Map<Long, Long> roomLodging = new HashMap<>();
        if (lodgingId != null) data.roomsForLodging(lodgingId).forEach(r -> roomLodging.put(r.getRoomId(), lodgingId));
        Map<Long, Member> members = new HashMap<>();
        if (query != null && !query.isBlank()) data.members().forEach(m -> members.put(m.getMemberId(), m));
        return data.reservations().stream()
            .filter(x -> status == null || status.equals(x.getStatus()))
            .filter(x -> type == null || ("reserve".equals(type) && "PENDING_PAYMENT".equals(x.getStatus()))
                || ("confirmed".equals(type) && List.of("CONFIRMED","COMPLETED","REFUNDING","REFUND_FAILED").contains(x.getStatus())))
            .filter(x -> lodgingId == null || roomLodging.containsKey(x.getRoomId()))
            .filter(x -> from == null || x.getCreatedAt() != null && !x.getCreatedAt().toLocalDate().isBefore(from))
            .filter(x -> to == null || x.getCreatedAt() != null && !x.getCreatedAt().toLocalDate().isAfter(to))
            .filter(x -> query == null || query.isBlank() || contains(x.getReservationNo(), query)
                || contains(x.getBookerName(), query) || contains(String.valueOf(x.getMemberId()), query)
                || memberMatches(members.get(x.getMemberId()), query))
            .map(AdminViews::of).toList();
    }
    @GetMapping("/orders/{id}")
    public Map<String,Object> order(@PathVariable Long id) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("order", AdminViews.of(data.reservation(id)));
        result.put("payments", AdminViews.list(data.paymentsForReservation(id)));
        result.put("paymentAttempts", jdbc.queryForList("""
            SELECT a.* FROM room_payment_attempt a JOIN room_payment p ON p.payment_id=a.payment_id
            WHERE p.reservation_id=? ORDER BY a.created_at DESC""", id));
        result.put("history", data.reservationHistory(id));
        return result;
    }
    @PostMapping("/orders/{id}/status")
    public Map<String,Object> orderStatus(@PathVariable Long id, @RequestBody StatusChange body, Principal principal) {
        data.setReservationStatus(id, body.status(), body.reason(), principal.getName());
        return order(id);
    }

    @GetMapping("/lodgings")
    public List<Map<String,Object>> lodgings(@RequestParam(required=false) String status,
        @RequestParam(required=false) Long ownerId, @RequestParam(required=false) String region,
        @RequestParam(required=false) String query, @RequestParam(required=false) String ownerName) {
        Map<Long, OwnerProfile> ownerById = ownersById();
        Map<Long, Member> memberById = membersById();
        return data.lodgings().stream().filter(x -> status != null ? status.equals(x.getStatus()) : !"DELETED".equals(x.getStatus()))
            .filter(x -> ownerId==null || ownerId.equals(x.getOwnerId()))
            .filter(x -> region==null || contains(x.getAddress(),region))
            .filter(x -> query==null || contains(x.getName(),query) || contains(x.getAddress(),query))
            .filter(x -> ownerName==null || ownerName.isBlank() || ownerMatches(ownerById.get(x.getOwnerId()), memberById, ownerName))
            .map(x -> lodgingView(x, ownerById, memberById)).toList();
    }
    @GetMapping("/lodgings/{id}")
    public Map<String,Object> lodging(@PathVariable Long id) {
        return Map.of("lodging",lodgingView(data.lodging(id), ownersById(), membersById()),"rooms",AdminViews.list(data.roomsForLodging(id)),
            "reservations",AdminViews.list(data.reservationsForLodging(id)));
    }
    @PostMapping("/lodgings/{id}/status")
    public Map<String,Object> lodgingStatus(@PathVariable Long id,@RequestBody StatusChange body) {
        data.setLodgingStatus(id,body.status(),body.reason()); return lodging(id);
    }
    @PostMapping("/lodgings/{id}/edit")
    public Map<String,Object> lodgingEdit(@PathVariable Long id,@RequestBody LodgingEdit body) {
        data.updateLodging(id,body.name(),body.address(),body.description()); return lodging(id);
    }
    @PostMapping("/lodgings/{id}/delete")
    public Map<String,Object> lodgingDelete(@PathVariable Long id, @RequestBody DeleteRequest body, Principal principal) {
        String status = data.deleteLodging(id, body.reason(), principal.getName());
        return Map.of("status", status, "message", "SUSPENDED".equals(status)
            ? "진행 중인 예약이 있어 삭제 대신 운영을 중지했어요." : "숙소를 삭제했어요.");
    }

    @GetMapping("/members")
    public List<MemberView> members(@RequestParam(required=false) String role,
        @RequestParam(required=false) String status,@RequestParam(required=false) String query,
        @RequestParam(required=false) LocalDate from,@RequestParam(required=false) LocalDate to) {
        return data.members().stream().filter(x -> MEMBER_ROLES.contains(x.getRoleCode()))
            .filter(x -> role==null || role.equals(x.getRoleCode()))
            .filter(x -> status==null || status.equals(x.getStatus()))
            .filter(x -> from==null || x.getCreatedAt()!=null && !x.getCreatedAt().toLocalDate().isBefore(from))
            .filter(x -> to==null || x.getCreatedAt()!=null && !x.getCreatedAt().toLocalDate().isAfter(to))
            .filter(x -> query==null || contains(x.getLoginId(),query) || contains(x.getNickname(),query)
                || contains(x.getEmail(),query)).map(this::memberView).toList();
    }
    @GetMapping("/members/{id}")
    public Map<String,Object> member(@PathVariable Long id) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("member",memberView(data.member(id)));
        OwnerProfile owner = data.ownerByMember(id);
        result.put("ownerProfile",owner == null ? null : AdminViews.of(owner));
        result.put("suspensions",AdminViews.list(data.suspensions(id)));
        result.put("ownerReviews",owner==null?List.of():AdminViews.list(data.reviewHistory(owner.getOwnerId())));
        result.put("ownerLodgings",owner==null?List.of():data.lodgings().stream()
            .filter(x -> Objects.equals(owner.getOwnerId(),x.getOwnerId())).map(AdminViews::of).toList());
        return result;
    }
    @PostMapping("/members/{id}/status")
    public Map<String,Object> memberStatus(@PathVariable Long id,@RequestBody MemberStatus body,Principal principal) {
        switch (body.status()) {
            case "SUSPENDED" -> data.suspendMember(id,body.reasonCode(),body.reason(),body.suspendedUntil(),principal.getName());
            case "ACTIVE" -> data.releaseMember(id);
            case "WITHDRAWN" -> data.withdrawMember(id,body.reason(),principal.getName());
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"지원하지 않는 회원 상태입니다.");
        }
        return member(id);
    }
    @GetMapping("/owners/pending")
    public List<Map<String,Object>> pendingOwners() { return data.owners().stream()
        .filter(x -> "PENDING".equals(x.getReviewStatusCode())).map(AdminViews::of).toList(); }
    @GetMapping("/owners")
    public List<Map<String,Object>> owners(@RequestParam(required=false) String reviewStatus,
        @RequestParam(required=false) String status, @RequestParam(required=false) String query) {
        Map<Long, Member> memberById = membersById();
        return data.owners().stream().filter(owner -> reviewStatus==null || reviewStatus.equals(owner.getReviewStatusCode()))
            .filter(owner -> {
                Member member = memberById.get(owner.getMemberId());
                return member != null && "OWNER".equals(member.getRoleCode())
                    && (status==null || status.equals(member.getStatus()))
                    && (query==null || query.isBlank() || contains(member.getLoginId(), query)
                        || contains(owner.getRepresentativeName(), query) || contains(owner.getBusinessName(), query));
            }).map(owner -> {
                Map<String,Object> view = new LinkedHashMap<>(AdminViews.of(owner));
                Member member = memberById.get(owner.getMemberId());
                view.put("loginId", member.getLoginId());
                view.put("status", member.getStatus());
                view.put("joinedAt", member.getCreatedAt());
                return view;
            }).toList();
    }
    @PostMapping("/owners/{id}/review")
    public Map<String,Object> ownerReview(@PathVariable Long id,@RequestBody OwnerDecision body,Principal principal) {
        data.reviewOwner(id,body.decision(),body.comment(),principal.getName()); return AdminViews.of(data.owner(id));
    }

    @GetMapping("/human-courses")
    public List<HumanCourseView> humanCourses(@RequestParam(required=false) String status,
        @RequestParam(required=false) String reportStatus,@RequestParam(required=false) String query,
        @RequestParam(required=false) Boolean reported, @RequestParam(required=false) String region,
        @RequestParam(required=false) LocalDate from, @RequestParam(required=false) LocalDate to) {
        List<HumanReportView> reports = data.humanReports();
        return data.humanCourses().stream().filter(x -> status==null || status.equals(x.getStatus()))
            .filter(x -> reported==null || reported == reports.stream().anyMatch(r -> Objects.equals(r.getCourseId(), x.getId())))
            .filter(x -> reportStatus==null || ("NONE".equals(reportStatus)
                ? reports.stream().noneMatch(r -> Objects.equals(r.getCourseId(),x.getId()))
                : reports.stream().anyMatch(r -> Objects.equals(r.getCourseId(),x.getId()) && reportStatus.equals(r.getStatus()))))
            .filter(x -> region==null || contains(x.getRegion(),region))
            .filter(x -> from==null || x.getCreatedAt()!=null && !x.getCreatedAt().toLocalDate().isBefore(from))
            .filter(x -> to==null || x.getCreatedAt()!=null && !x.getCreatedAt().toLocalDate().isAfter(to))
            .filter(x -> query==null || contains(x.getTitle(),query) || contains(x.getDestination(),query)).toList();
    }
    @GetMapping("/human-courses/{id}")
    public Map<String,Object> humanCourse(@PathVariable Long id) {
        return Map.of("post",data.humanCourse(id),"reports",data.humanReports().stream()
            .filter(x -> Objects.equals(x.getCourseId(),id)).toList(),"history",data.humanHistory(id));
    }
    @GetMapping("/human-reports")
    public List<HumanReportView> humanReports() { return data.humanReports(); }
    @PostMapping("/human-courses/{id}/status")
    public Map<String,Object> humanStatus(@PathVariable Long id,@RequestBody StatusChange body,Principal principal) {
        data.setHumanCourseStatus(id,body.status(),principal.getName(),body.reason()); return humanCourse(id);
    }
    @PostMapping("/human-courses/{id}/feature")
    public Map<String,Object> humanFeature(@PathVariable Long id,@RequestBody FeatureChange body,Principal principal) {
        data.setHumanFeatured(id,body.featured(),principal.getName()); return humanCourse(id);
    }
    @PostMapping("/human-reports/{id}/resolve")
    public HumanReportView resolveReport(@PathVariable Long id,@RequestBody StatusChange body,Principal principal) {
        data.resolveHumanReport(id,body.status(),principal.getName(),body.reason());
        return data.humanReports().stream().filter(x -> Objects.equals(id,x.getId())).findFirst().orElseThrow();
    }

    @GetMapping("/ai-courses")
    public List<Map<String,Object>> aiCourses(@RequestParam(required=false) String status,@RequestParam(required=false) String query) {
        return data.courses().stream().filter(x -> status==null || status.equalsIgnoreCase(x.getStatus()))
            .filter(x -> query==null || contains(x.getTitle(),query) || contains(x.getDestination(),query))
            .map(AdminViews::of).toList();
    }
    @GetMapping("/ai-courses/{id}")
    public Map<String,Object> aiCourse(@PathVariable Long id) {
        return Map.of("course",AdminViews.of(data.course(id)),"reservations",data.reservations().stream()
            .filter(x -> Objects.equals(x.getCourseId(),id)).map(AdminViews::of).toList());
    }
    @GetMapping("/ai-courses/errors")
    public List<Map<String,Object>> aiErrors() { return aiCourses("failed",null); }
    @GetMapping("/ai-courses/criteria")
    public List<RecommendationCriteria> criteria() { return data.criteria(); }
    @PostMapping("/ai-courses/criteria/{id}")
    public RecommendationCriteria criteria(@PathVariable Long id,@RequestBody CriteriaChange body) {
        data.updateCriterion(id,body.value());
        return data.criteria().stream().filter(x -> Objects.equals(x.getId(),id)).findFirst().orElseThrow();
    }
    @GetMapping("/ai-courses/criteria/{id}/history")
    public List<CriteriaHistoryView> criteriaHistory(@PathVariable Long id) {
        return data.criteriaHistory(id).stream().map(h -> {
            Member admin = data.member(h.getAdminId());
            return new CriteriaHistoryView(h.getId(), h.getOldValue(), h.getNewValue(),
                admin.getLoginId(), admin.getNickname(), h.getCreatedAt());
        }).toList();
    }

    @GetMapping("/tour-data")
    public Map<String,Object> tourData(@RequestParam(defaultValue="47130") String region) {
        List<Map<String,Object>> hubs = safe(() -> tour.hubTop(region,20), List.of());
        // 위 숫자 카드는 아래 관광지 목록과 같은 전체 기준 (예전에는 경주만 셈)
        Map<String,Object> counts = new LinkedHashMap<>(jdbc.queryForMap("""
            SELECT (SELECT COUNT(*) FROM place) AS places,
                   (SELECT COUNT(DISTINCT i.place_id) FROM place_image i) AS withImage
            """));
        // 중심·연관 관광지 표는 팀 SQL로 만드는 표라 DB에 없을 수 있음 → 없으면 0
        counts.put("hubRanks", safe(() -> jdbc.queryForObject("SELECT COUNT(*) FROM place_hub_rank", Long.class), 0L));
        counts.put("related", safe(() -> jdbc.queryForObject("SELECT COUNT(*) FROM related_attraction_rank", Long.class), 0L));
        Map<String,Object> byType = new LinkedHashMap<>();
        jdbc.queryForList("SELECT place_type AS type, COUNT(*) AS n FROM place GROUP BY place_type")
            .forEach(row -> byType.put(String.valueOf(row.get("type")), row.get("n")));
        Map<String,Object> imageByType = new LinkedHashMap<>();
        jdbc.queryForList("""
            SELECT p.place_type AS type, COUNT(DISTINCT p.place_id) AS n
            FROM place p JOIN place_image i ON i.place_id=p.place_id GROUP BY p.place_type""")
            .forEach(row -> imageByType.put(String.valueOf(row.get("type")), row.get("n")));
        counts.put("byType", byType);
        counts.put("imageByType", imageByType);
        // 중심·연관 관광지 카드 아래 작은 글씨: 지역별 개수 · 기준 월 · 기준 관광지 수
        counts.put("hubByRegion", safe(() -> jdbc.queryForList("""
            SELECT r.full_name AS name, COUNT(*) AS n FROM place_hub_rank h JOIN region r ON r.region_id=h.region_id
            GROUP BY r.full_name ORDER BY n DESC LIMIT 3"""), List.of()));
        counts.put("hubMonths", safe(() -> jdbc.queryForList("SELECT DISTINCT base_ym FROM place_hub_rank ORDER BY base_ym DESC LIMIT 3", String.class), List.of()));
        counts.put("relatedByRegion", safe(() -> jdbc.queryForList("""
            SELECT r.full_name AS name, COUNT(*) AS n FROM related_attraction_rank x JOIN place p ON p.place_id=x.base_place_id
            JOIN region r ON r.region_id=p.region_id GROUP BY r.full_name ORDER BY n DESC LIMIT 3"""), List.of()));
        counts.put("relatedBases", safe(() -> jdbc.queryForObject("SELECT COUNT(DISTINCT base_place_id) FROM related_attraction_rank", Long.class), 0L));
        return Map.of("regions",safe(tour::supportedRegions, List.of()),"counts",counts,"hubs",hubs,"logs",safe(tour::recentLogs, List.of()));
    }
    @PostMapping("/tour-data/sync")
    public TourSyncService.SyncResult tourSync(@RequestBody TourRun body) { return tour.sync(body.region()); }

    @GetMapping("/regions")
    public List<Map<String,Object>> regions() {
        return jdbc.queryForList("""
            SELECT region_id AS regionId, parent_region_id AS parentRegionId, full_name AS fullName,
                   region_level AS regionLevel
            FROM region WHERE region_level IN ('SIDO', 'SIGUNGU')
            ORDER BY full_name
            """);
    }

    @GetMapping("/places")
    public Map<String,Object> places(@RequestParam(required=false) Long sido,
        @RequestParam(required=false) Long sigungu, @RequestParam(required=false) String type,
        @RequestParam(required=false) String query, @RequestParam(defaultValue="1") int page,
        @RequestParam(defaultValue="10") int size) {
        if (page < 1 || size < 1 || size > 100) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "페이지 값이 올바르지 않습니다.");
        if (type != null && !type.isBlank() && !List.of("ATTRACTION", "CULTURE", "LEISURE", "RESTAURANT", "SHOPPING").contains(type))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "유형이 올바르지 않습니다.");
        String where = " FROM place p JOIN region r ON r.region_id=p.region_id WHERE 1=1";
        List<Object> params = new java.util.ArrayList<>();
        if (sido != null) { where += " AND (r.region_id=? OR r.parent_region_id=?)"; params.add(sido); params.add(sido); }
        if (sigungu != null) { where += " AND r.region_id=?"; params.add(sigungu); }
        if (type != null && !type.isBlank()) { where += " AND p.place_type=?"; params.add(type); }
        if (query != null && !query.isBlank()) { where += " AND LOWER(p.name) LIKE ?"; params.add("%" + query.trim().toLowerCase() + "%"); }
        Long total = jdbc.queryForObject("SELECT COUNT(*)" + where, Long.class, params.toArray());
        List<Object> pageParams = new java.util.ArrayList<>(params);
        pageParams.add(size); pageParams.add((page - 1) * size);
        List<Map<String,Object>> rows = jdbc.queryForList("""
            SELECT p.place_id AS placeId, p.name, p.place_type AS placeType, r.full_name AS region,
                   p.address, EXISTS(SELECT 1 FROM place_image i WHERE i.place_id=p.place_id) AS hasImage
            """ + where + " ORDER BY p.place_id DESC LIMIT ? OFFSET ?", pageParams.toArray());
        return Map.of("rows", rows, "total", total == null ? 0 : total, "page", page, "size", size);
    }

    private static final List<String> MEMBER_ROLES = List.of("USER", "ADMIN");
    private Map<Long, OwnerProfile> ownersById() {
        Map<Long, OwnerProfile> map = new HashMap<>();
        data.owners().forEach(owner -> map.put(owner.getOwnerId(), owner));
        return map;
    }
    private Map<Long, Member> membersById() {
        Map<Long, Member> map = new HashMap<>();
        data.members().forEach(member -> map.put(member.getMemberId(), member));
        return map;
    }
    /** 표가 없는 DB(예: 새로 복구한 DB)에서도 관광지 데이터 화면 전체가 멈추지 않게, 실패한 숫자만 기본값으로 */
    private <T> T safe(java.util.function.Supplier<T> query, T fallback) {
        try { return query.get(); } catch (org.springframework.dao.DataAccessException e) { return fallback; }
    }

    private Map<String,Object> lodgingView(com.smarttraveler.domain.entity.Lodging lodging,
        Map<Long, OwnerProfile> ownerById, Map<Long, Member> memberById) {
        Map<String,Object> view = new LinkedHashMap<>(AdminViews.of(lodging));
        OwnerProfile owner = ownerById.get(lodging.getOwnerId());
        Member member = owner == null ? null : memberById.get(owner.getMemberId());
        String name = owner == null ? null : !blank(owner.getRepresentativeName()) ? owner.getRepresentativeName()
            : !blank(owner.getBusinessName()) ? owner.getBusinessName() : member == null ? null : member.getNickname();
        view.put("ownerName", name);
        return view;
    }
    private static boolean ownerMatches(OwnerProfile owner, Map<Long, Member> memberById, String query) {
        if (owner == null) return false;
        Member member = memberById.get(owner.getMemberId());
        return contains(owner.getRepresentativeName(), query) || contains(owner.getBusinessName(), query)
            || member != null && contains(member.getNickname(), query);
    }
    private static boolean blank(String value) { return value == null || value.isBlank(); }

    private static boolean contains(String text,String q) { return text!=null && text.toLowerCase().contains(q.toLowerCase()); }
    private static boolean memberMatches(Member member, String q) {
        return member != null && (contains(member.getLoginId(), q) || contains(member.getNickname(), q));
    }
    private MemberView memberView(Member member) {
        MemberSuspensionHistory active = "SUSPENDED".equals(member.getStatus()) ? data.suspensions(member.getMemberId()).stream()
            .filter(h -> h.getReleasedAt() == null).findFirst().orElse(null) : null;
        return new MemberView(member, active == null ? null : active.getSuspendedUntil());
    }
    public record StatusChange(String status,String reason) {}
    public record MemberStatus(String status,String reasonCode,String reason,LocalDateTime suspendedUntil) {}
    public record LodgingEdit(String name,String address,String description) {}
    public record DeleteRequest(String reason) {}
    public record OwnerDecision(String decision,String comment) {}
    public record FeatureChange(boolean featured) {}
    public record CriteriaChange(String value) {}
    public record CriteriaHistoryView(Long id, String oldValue, String newValue,
        String adminLoginId, String adminNickname, LocalDateTime createdAt) {}
    public record TourRun(String region) {}
    public record MemberView(Long memberId,String loginId,String nickname,String email,String roleCode,
        String status,LocalDateTime createdAt,LocalDateTime deletedAt,
        LocalDateTime suspendedUntil) {
        MemberView(Member m, LocalDateTime until) { this(m.getMemberId(),m.getLoginId(),m.getNickname(),m.getEmail(),
            m.getRoleCode(),m.getStatus(),m.getCreatedAt(),m.getDeletedAt(),until); }
    }
}
