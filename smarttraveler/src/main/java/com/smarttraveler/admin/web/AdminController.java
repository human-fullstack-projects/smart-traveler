package com.smarttraveler.admin.web;

import com.smarttraveler.admin.data.*;
import com.smarttraveler.admin.service.AdminDataService;
import java.security.Principal;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class AdminController {
    private final AdminDataService data;

    public AdminController(AdminDataService data) { this.data = data; }

    @GetMapping({"", "/"})
    public String dashboard(Model model) {
        model.addAttribute("memberCount", data.members().size());
        model.addAttribute("orderCount", data.reservations().size());
        model.addAttribute("lodgingCount", data.lodgings().size());
        model.addAttribute("humanCount", data.humanCourses().size());
        model.addAttribute("stats", java.util.Map.of(
            "orderCount", data.reservations().size(),
            "pendingLodgingCount", data.lodgings().stream().filter(x -> "PENDING".equals(x.getStatus())).count(),
            "suspendedMemberCount", data.members().stream().filter(x -> "SUSPENDED".equals(x.getStatus())).count(),
            "openReportCount", data.humanReports().stream().filter(x -> "PENDING".equals(x.getStatus())).count()));
        return "admin/dashboard";
    }

    @GetMapping("/orders")
    public String orders(@RequestParam(required = false) Long id,
                         @RequestParam(required = false) String type,
                         @RequestParam(required = false) String status,
                         @RequestParam(required = false) Long lodgingId,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                         @RequestParam(required = false) String query, Model model) {
        Map<Long, Long> lodgingByRoom = data.lodgings().stream()
            .flatMap(l -> data.roomsForLodging(l.getLodgingId()).stream()
                .map(r -> Map.entry(r.getRoomId(), l.getLodgingId())))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a,b) -> a));
        Map<Long, Member> memberById = data.members().stream()
            .collect(Collectors.toMap(Member::getMemberId, x -> x));
        List<Reservation> orders = data.reservations().stream()
            .filter(x -> !StringUtils.hasText(type) ||
                ("reserve".equals(type) && "PENDING_PAYMENT".equals(x.getStatus())) ||
                ("confirmed".equals(type) && List.of("CONFIRMED", "COMPLETED", "REFUNDING", "REFUND_FAILED").contains(x.getStatus())))
            .filter(x -> !StringUtils.hasText(status) || status.equals(x.getStatus()))
            .filter(x -> lodgingId == null || lodgingId.equals(lodgingByRoom.get(x.getRoomId())))
            .filter(x -> from == null || x.getCreatedAt() != null && !x.getCreatedAt().toLocalDate().isBefore(from))
            .filter(x -> to == null || x.getCreatedAt() != null && !x.getCreatedAt().toLocalDate().isAfter(to))
            .filter(x -> !StringUtils.hasText(query) || contains(x.getReservationNo(), query)
                || contains(x.getBookerName(), query)
                || (memberById.containsKey(x.getMemberId()) &&
                    (contains(memberById.get(x.getMemberId()).getLoginId(), query)
                     || contains(memberById.get(x.getMemberId()).getNickname(), query))))
            .toList();
        model.addAttribute("orders", orders);
        Reservation selectedOrder = id == null ? null : data.reservation(id);
        model.addAttribute("selectedOrder", selectedOrder);
        Long selectedLodgingId = selectedOrder == null ? null : lodgingByRoom.get(selectedOrder.getRoomId());
        model.addAttribute("selectedOrderLodging", selectedLodgingId == null ? null : data.lodging(selectedLodgingId));
        model.addAttribute("selectedOrderRoom", selectedLodgingId == null ? null : data.roomsForLodging(selectedLodgingId)
            .stream().filter(r -> r.getRoomId().equals(selectedOrder.getRoomId())).findFirst().orElse(null));
        model.addAttribute("selectedOrderCourse", selectedOrder == null || selectedOrder.getCourseId() == null
            ? null : data.course(selectedOrder.getCourseId()));
        model.addAttribute("payments", id == null ? List.of() : data.paymentsForReservation(id));
        model.addAttribute("orderHistory", id == null ? List.of() : data.reservationHistory(id));
        model.addAttribute("status", status);
        model.addAttribute("type", type);
        model.addAttribute("lodgingId", lodgingId);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("query", query);
        model.addAttribute("allLodgings", data.lodgings());
        model.addAttribute("lodgings", data.lodgings());
        return "admin/orders";
    }

    @PostMapping("/orders/{id}/status")
    public String orderStatus(@PathVariable Long id, @RequestParam String status,
                              @RequestParam(required = false) String reason,
                              Principal principal, RedirectAttributes redirect) {
        attempt(redirect, () -> data.setReservationStatus(id, status, reason, principal.getName()));
        return "redirect:/admin/orders?id=" + id;
    }

    @GetMapping("/lodgings")
    public String lodgings(@RequestParam(required = false) Long id,
                           @RequestParam(required = false) Long ownerId,
                           @RequestParam(required = false) String region,
                           @RequestParam(required = false) String status,
                           @RequestParam(required = false) String query, Model model) {
        model.addAttribute("lodgings", data.lodgings().stream()
            .filter(x -> ownerId == null || ownerId.equals(x.getOwnerId()))
            .filter(x -> !StringUtils.hasText(region) || contains(x.getAddress(), region))
            .filter(x -> !StringUtils.hasText(status) || status.equals(x.getStatus()))
            .filter(x -> !StringUtils.hasText(query) || contains(x.getName(), query)
                || contains(x.getAddress(), query)).toList());
        model.addAttribute("selectedLodging", id == null ? null : data.lodging(id));
        model.addAttribute("owners", data.owners());
        model.addAttribute("rooms", id == null ? List.of() : data.roomsForLodging(id));
        model.addAttribute("lodgingReservations", id == null ? List.of() : data.reservationsForLodging(id));
        model.addAttribute("status", status);
        model.addAttribute("ownerId", ownerId);
        model.addAttribute("region", region);
        model.addAttribute("query", query);
        return "admin/lodgings";
    }

    @PostMapping("/lodgings/{id}/status")
    public String lodgingStatus(@PathVariable Long id, @RequestParam String status,
                                @RequestParam(required = false) String reason,
                                RedirectAttributes redirect) {
        attempt(redirect, () -> data.setLodgingStatus(id, status, reason));
        return "redirect:/admin/lodgings?id=" + id;
    }

    @PostMapping("/lodgings/{id}/edit")
    public String lodgingEdit(@PathVariable Long id, @RequestParam String name,
                              @RequestParam(required = false) String address,
                              @RequestParam(required = false) String description,
                              RedirectAttributes redirect) {
        attempt(redirect, () -> data.updateLodging(id, name, address, description));
        return "redirect:/admin/lodgings?id=" + id;
    }

    @GetMapping("/members")
    public String members(@RequestParam(required = false) Long id,
                          @RequestParam(required = false) String role,
                          @RequestParam(required = false) String status,
                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                          @RequestParam(required = false) String query, Model model) {
        model.addAttribute("members", data.members().stream()
            .filter(x -> !StringUtils.hasText(role) || role.equals(x.getRoleCode()))
            .filter(x -> !StringUtils.hasText(status) || status.equals(x.getStatus()))
            .filter(x -> from == null || x.getCreatedAt() != null && !x.getCreatedAt().toLocalDate().isBefore(from))
            .filter(x -> to == null || x.getCreatedAt() != null && !x.getCreatedAt().toLocalDate().isAfter(to))
            .filter(x -> !StringUtils.hasText(query) || contains(x.getLoginId(), query)
                || contains(x.getNickname(), query) || contains(x.getEmail(), query)).toList());
        model.addAttribute("selectedMember", id == null ? null : data.member(id));
        var owner = id == null ? null : data.ownerByMember(id);
        model.addAttribute("ownerProfile", owner);
        model.addAttribute("ownerLodgings", owner == null ? List.of() : data.lodgings().stream()
            .filter(x -> owner.getOwnerId().equals(x.getOwnerId())).toList());
        model.addAttribute("ownerReviewHistory", owner == null ? List.of() : data.reviewHistory(owner.getOwnerId()));
        model.addAttribute("suspensionHistory", id == null ? List.of() : data.suspensions(id));
        model.addAttribute("owners", data.owners());
        model.addAttribute("role", role);
        model.addAttribute("status", status);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("query", query);
        return "admin/members";
    }

    @PostMapping("/members/{id}/status")
    public String memberStatus(@PathVariable Long id, @RequestParam String status,
                               @RequestParam(required = false) String reasonCode,
                               @RequestParam(required = false) String reason,
                               Principal principal, RedirectAttributes redirect) {
        attempt(redirect, () -> {
            switch (status) {
                case "SUSPENDED" -> data.suspendMember(id, reasonCode, reason, principal.getName());
                case "ACTIVE" -> data.releaseMember(id);
                case "WITHDRAWN" -> data.withdrawMember(id, reason, principal.getName());
                default -> throw new IllegalArgumentException("지원하지 않는 회원 상태");
            }
        });
        return "redirect:/admin/members?id=" + id;
    }

    @PostMapping("/owners/{id}/review")
    public String ownerReview(@PathVariable Long id, @RequestParam String decision,
                              @RequestParam(required = false) String comment,
                              Principal principal, RedirectAttributes redirect) {
        attempt(redirect, () -> data.reviewOwner(id, decision, comment, principal.getName()));
        return "redirect:/admin/members";
    }

    @GetMapping("/human-courses")
    public String humanCourses(@RequestParam(required = false) Long id,
                               @RequestParam(required = false) String status,
                               @RequestParam(required = false) String reportStatus,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                               @RequestParam(required = false) String query, Model model) {
        var allReports = data.humanReports();
        model.addAttribute("humanCourses", data.humanCourses().stream()
            .filter(x -> !StringUtils.hasText(status) || status.equals(x.getStatus()))
            .filter(x -> {
                if (!StringUtils.hasText(reportStatus)) return true;
                if ("NONE".equals(reportStatus))
                    return allReports.stream().noneMatch(r -> x.getId().equals(r.getCourseId()));
                return allReports.stream().anyMatch(r -> x.getId().equals(r.getCourseId())
                    && reportStatus.equals(r.getStatus()));
            })
            .filter(x -> from == null || x.getCreatedAt() != null && !x.getCreatedAt().toLocalDate().isBefore(from))
            .filter(x -> to == null || x.getCreatedAt() != null && !x.getCreatedAt().toLocalDate().isAfter(to))
            .filter(x -> !StringUtils.hasText(query) || contains(x.getTitle(), query)
                || contains(x.getDestination(), query)).toList());
        model.addAttribute("reports", allReports.stream()
            .filter(x -> id == null || id.equals(x.getCourseId())).toList());
        model.addAttribute("selectedHumanCourse", id == null ? null : data.humanCourse(id));
        model.addAttribute("humanHistory", id == null ? List.of() : data.humanHistory(id));
        model.addAttribute("status", status);
        model.addAttribute("reportStatus", reportStatus);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("query", query);
        return "admin/human-courses";
    }

    @PostMapping("/human-courses/{id}/status")
    public String humanStatus(@PathVariable Long id, @RequestParam String status,
                              @RequestParam(required = false) String reason,
                              Principal principal, RedirectAttributes redirect) {
        attempt(redirect, () -> data.setHumanCourseStatus(id, status, principal.getName(), reason));
        return "redirect:/admin/human-courses?id=" + id;
    }

    @PostMapping("/human-courses/{id}/feature")
    public String humanFeature(@PathVariable Long id, @RequestParam boolean featured,
                               Principal principal, RedirectAttributes redirect) {
        attempt(redirect, () -> data.setHumanFeatured(id, featured, principal.getName()));
        return "redirect:/admin/human-courses?id=" + id;
    }

    @PostMapping("/human-reports/{id}/resolve")
    public String humanReport(@PathVariable Long id, @RequestParam String status,
                              @RequestParam(required = false) String reason,
                              Principal principal, RedirectAttributes redirect) {
        Long courseId = data.humanReports().stream().filter(x -> id.equals(x.getId()))
            .map(HumanReport::getCourseId).findFirst().orElse(null);
        attempt(redirect, () -> data.resolveHumanReport(id, status, principal.getName(), reason));
        return courseId == null ? "redirect:/admin/human-courses"
            : "redirect:/admin/human-courses?id=" + courseId;
    }

    @GetMapping("/ai-courses")
    public String aiCourses(@RequestParam(required = false) Long id,
                            @RequestParam(required = false) String status,
                            @RequestParam(required = false) String query, Model model) {
        List<Course> courses = data.courses().stream()
            .filter(x -> !StringUtils.hasText(status) || status.equalsIgnoreCase(x.getStatus()))
            .filter(x -> !StringUtils.hasText(query) || contains(x.getTitle(), query)
                || contains(x.getDestination(), query)).toList();
        model.addAttribute("aiCourses", courses);
        Course selected = id == null ? null : data.course(id);
        model.addAttribute("selectedAiCourse", selected);
        if (selected != null) {
            model.addAttribute("selectedAiMember", selected.getMemberNo() == null ? null
                : data.members().stream().filter(m -> selected.getMemberNo().equals(m.getMemberId())).findFirst().orElse(null));
            model.addAttribute("selectedAiParent", null);
            model.addAttribute("selectedAiReservations", data.reservations().stream()
                .filter(r -> selected.getId().equals(r.getCourseId())).toList());
        }
        model.addAttribute("aiErrors", courses.stream().filter(x -> "failed".equalsIgnoreCase(x.getStatus())).toList());
        model.addAttribute("criteria", data.criteria());
        model.addAttribute("status", status);
        model.addAttribute("query", query);
        return "admin/ai-courses";
    }

    @PostMapping("/ai-courses/criteria")
    public String criteria(@RequestParam Long id, @RequestParam String value,
                           RedirectAttributes redirect) {
        attempt(redirect, () -> data.updateCriterion(id, value));
        return "redirect:/admin/ai-courses";
    }

    private static boolean contains(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT));
    }

    private static void attempt(RedirectAttributes redirect, Runnable action) {
        try { action.run(); redirect.addFlashAttribute("message", "처리 결과를 저장했습니다."); }
        catch (IllegalArgumentException | IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
    }
}
