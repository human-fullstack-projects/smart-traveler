package com.smarttraveler.admin.controller;

import com.smarttraveler.admin.repository.*;
import com.smarttraveler.admin.dto.AdminViews;
import com.smarttraveler.admin.dto.AdminViews.HumanCourseView;
import com.smarttraveler.admin.dto.AdminViews.HumanReportView;
import com.smarttraveler.domain.entity.Member;
import com.smarttraveler.domain.entity.RecommendationCriteria;
import com.smarttraveler.admin.service.AdminDataService;
import com.smarttraveler.tour.service.TourSyncService;
import java.security.Principal;
import java.time.LocalDate;
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
        return Map.of("members", data.members().size(), "orders", data.reservations().size(),
            "lodgings", data.lodgings().size(), "humanCourses", data.humanCourses().size(),
            "pendingOwners", data.owners().stream().filter(x -> "PENDING".equals(x.getReviewStatusCode())).count(),
            "openReports", data.humanReports().stream().filter(x -> "PENDING".equals(x.getStatus())).count());
    }

    @GetMapping("/orders")
    public List<Map<String,Object>> orders(@RequestParam(required=false) String status,
        @RequestParam(required=false) String type, @RequestParam(required=false) String query,
        @RequestParam(required=false) Long lodgingId,
        @RequestParam(required=false) LocalDate from, @RequestParam(required=false) LocalDate to) {
        var roomLodging = new java.util.HashMap<Long,Long>();
        if (lodgingId != null) data.roomsForLodging(lodgingId).forEach(r -> roomLodging.put(r.getRoomId(), lodgingId));
        return data.reservations().stream()
            .filter(x -> status == null || status.equals(x.getStatus()))
            .filter(x -> type == null || ("reserve".equals(type) && "PENDING_PAYMENT".equals(x.getStatus()))
                || ("confirmed".equals(type) && List.of("CONFIRMED","COMPLETED","REFUNDING","REFUND_FAILED").contains(x.getStatus())))
            .filter(x -> lodgingId == null || roomLodging.containsKey(x.getRoomId()))
            .filter(x -> from == null || x.getCreatedAt() != null && !x.getCreatedAt().toLocalDate().isBefore(from))
            .filter(x -> to == null || x.getCreatedAt() != null && !x.getCreatedAt().toLocalDate().isAfter(to))
            .filter(x -> query == null || contains(x.getReservationNo(), query) || contains(x.getBookerName(), query))
            .map(AdminViews::of).toList();
    }
    @GetMapping("/orders/{id}")
    public Map<String,Object> order(@PathVariable Long id) {
        var result = new java.util.LinkedHashMap<String,Object>();
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
        @RequestParam(required=false) String query) {
        return data.lodgings().stream().filter(x -> status==null || status.equals(x.getStatus()))
            .filter(x -> ownerId==null || ownerId.equals(x.getOwnerId()))
            .filter(x -> region==null || contains(x.getAddress(),region))
            .filter(x -> query==null || contains(x.getName(),query) || contains(x.getAddress(),query))
            .map(AdminViews::of).toList();
    }
    @GetMapping("/lodgings/{id}")
    public Map<String,Object> lodging(@PathVariable Long id) {
        return Map.of("lodging",AdminViews.of(data.lodging(id)),"rooms",AdminViews.list(data.roomsForLodging(id)),
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

    @GetMapping("/members")
    public List<MemberView> members(@RequestParam(required=false) String role,
        @RequestParam(required=false) String status,@RequestParam(required=false) String query,
        @RequestParam(required=false) LocalDate from,@RequestParam(required=false) LocalDate to) {
        return data.members().stream().filter(x -> role==null || role.equals(x.getRoleCode()))
            .filter(x -> status==null || status.equals(x.getStatus()))
            .filter(x -> from==null || x.getCreatedAt()!=null && !x.getCreatedAt().toLocalDate().isBefore(from))
            .filter(x -> to==null || x.getCreatedAt()!=null && !x.getCreatedAt().toLocalDate().isAfter(to))
            .filter(x -> query==null || contains(x.getLoginId(),query) || contains(x.getNickname(),query)
                || contains(x.getEmail(),query)).map(MemberView::new).toList();
    }
    @GetMapping("/members/{id}")
    public Map<String,Object> member(@PathVariable Long id) {
        var result=new java.util.LinkedHashMap<String,Object>();
        result.put("member",new MemberView(data.member(id)));
        var owner=data.ownerByMember(id);
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
            case "SUSPENDED" -> data.suspendMember(id,body.reasonCode(),body.reason(),principal.getName());
            case "ACTIVE" -> data.releaseMember(id);
            case "WITHDRAWN" -> data.withdrawMember(id,body.reason(),principal.getName());
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"지원하지 않는 회원 상태입니다.");
        }
        return member(id);
    }
    @GetMapping("/owners/pending")
    public List<Map<String,Object>> pendingOwners() { return data.owners().stream()
        .filter(x -> "PENDING".equals(x.getReviewStatusCode())).map(AdminViews::of).toList(); }
    @PostMapping("/owners/{id}/review")
    public Map<String,Object> ownerReview(@PathVariable Long id,@RequestBody OwnerDecision body,Principal principal) {
        data.reviewOwner(id,body.decision(),body.comment(),principal.getName()); return AdminViews.of(data.owner(id));
    }

    @GetMapping("/human-courses")
    public List<HumanCourseView> humanCourses(@RequestParam(required=false) String status,
        @RequestParam(required=false) String reportStatus,@RequestParam(required=false) String query) {
        var reports=data.humanReports();
        return data.humanCourses().stream().filter(x -> status==null || status.equals(x.getStatus()))
            .filter(x -> reportStatus==null || ("NONE".equals(reportStatus)
                ? reports.stream().noneMatch(r -> Objects.equals(r.getCourseId(),x.getId()))
                : reports.stream().anyMatch(r -> Objects.equals(r.getCourseId(),x.getId()) && reportStatus.equals(r.getStatus()))))
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

    @GetMapping("/tour-data")
    public Map<String,Object> tourData(@RequestParam(defaultValue="47130") String region) {
        return Map.of("regions",tour.supportedRegions(),"counts",tour.counts(region),
            "hubs",tour.hubTop(region,20),"logs",tour.recentLogs());
    }
    @PostMapping("/tour-data/sync")
    public TourSyncService.SyncResult tourSync(@RequestBody TourRun body) { return tour.sync(body.region()); }

    private static boolean contains(String text,String q) { return text!=null && text.toLowerCase().contains(q.toLowerCase()); }
    public record StatusChange(String status,String reason) {}
    public record MemberStatus(String status,String reasonCode,String reason) {}
    public record LodgingEdit(String name,String address,String description) {}
    public record OwnerDecision(String decision,String comment) {}
    public record FeatureChange(boolean featured) {}
    public record CriteriaChange(String value) {}
    public record TourRun(String region) {}
    public record MemberView(Long memberId,String loginId,String nickname,String email,String roleCode,
        String status,java.time.LocalDateTime createdAt,java.time.LocalDateTime deletedAt) {
        MemberView(Member m) { this(m.getMemberId(),m.getLoginId(),m.getNickname(),m.getEmail(),
            m.getRoleCode(),m.getStatus(),m.getCreatedAt(),m.getDeletedAt()); }
    }
}
