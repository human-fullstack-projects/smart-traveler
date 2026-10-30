package com.smarttraveler.admin.service;

import com.smarttraveler.admin.data.*;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

@Service @RequiredArgsConstructor
public class AdminDataService {
    private final MemberRepository members;
    private final OwnerProfileRepository owners;
    private final OwnerReviewHistoryRepository reviews;
    private final MemberSuspensionHistoryRepository suspensions;
    private final LodgingRepository lodgings;
    private final RoomRepository rooms;
    private final ReservationRepository reservations;
    private final ReservationActionHistoryRepository reservationActions;
    private final PaymentRepository payments;
    private final CourseRepository courses;
    private final CourseTemplateRepository templates;
    private final TravelPlanVersionRepository versions;
    private final HumanCourseRepository humanCourses;
    private final HumanReportRepository humanReports;
    private final HumanActionHistoryRepository humanActions;
    private final RecommendationCriteriaRepository criteria;
    private final RecommendationCriteriaHistoryRepository criteriaHistory;
    private final JdbcTemplate jdbc;

    public List<Member> members() { return members.findAll(); }
    public Member member(Long id) { return members.findById(id).orElseThrow(() -> missing("회원")); }
    public List<OwnerProfile> owners() { return owners.findAll(); }
    public OwnerProfile owner(Long id) { return owners.findById(id).orElseThrow(() -> missing("업주")); }
    public OwnerProfile ownerByMember(Long id) { return owners.findByMemberId(id).orElse(null); }
    public List<OwnerReviewHistory> reviewHistory(Long ownerId) { return reviews.findByOwnerIdOrderByCreatedAtDesc(ownerId); }
    public List<MemberSuspensionHistory> suspensions(Long memberId) { return suspensions.findByMemberIdOrderByCreatedAtDesc(memberId); }
    public List<Lodging> lodgings() { return lodgings.findAll(); }
    public Lodging lodging(Long id) { return lodgings.findById(id).orElseThrow(() -> missing("숙소")); }
    public List<Room> roomsForLodging(Long id) { return rooms.findByLodgingId(id); }
    public List<Reservation> reservationsForLodging(Long id) {
        List<Long> roomIds = roomsForLodging(id).stream().map(Room::getRoomId).toList();
        return roomIds.isEmpty() ? List.of() : reservations.findByRoomIdIn(roomIds);
    }
    public List<Reservation> reservations() { return reservations.findAll(); }
    public Reservation reservation(Long id) { return reservations.findById(id).orElseThrow(() -> missing("예약")); }
    public List<Payment> paymentsForReservation(Long id) { return payments.findByReservationIdOrderByCreatedAtDesc(id); }
    public List<ReservationActionHistory> reservationHistory(Long id) { return reservationActions.findByReservationIdOrderByCreatedAtDesc(id); }
    public List<Course> courses() { return courses.findAll(); }
    public Course course(Long id) { return courses.findById(id).orElseThrow(() -> missing("코스")); }
    public List<CourseTemplate> templates() { return templates.findAll(); }
    public TravelPlanVersion version(Long id) { return versions.findById(id).orElseThrow(() -> missing("코스 버전")); }
    public List<HumanCourse> humanCourses() { return humanCourses.findAll(); }
    public HumanCourse humanCourse(Long id) { return humanCourses.findById(id).orElseThrow(() -> missing("휴먼 코스")); }
    public List<HumanReport> humanReports() { return humanReports.findAll(); }
    public List<HumanActionHistory> humanHistory(Long courseId) { return humanActions.findByCourseIdOrderByCreatedAtDesc(courseId); }
    public List<RecommendationCriteria> criteria() { return criteria.findAll(); }
    public List<RecommendationCriteriaHistory> criteriaHistory(Long id) { return criteriaHistory.findByCriteriaIdOrderByCreatedAtDesc(id); }

    @Transactional
    public void reviewOwner(Long id, String status, String comment, String reviewerLogin) {
        if (!Set.of("APPROVED", "REJECTED").contains(status)) throw new IllegalArgumentException("승인 또는 반려만 선택할 수 있습니다.");
        if ("REJECTED".equals(status) && blank(comment)) throw new IllegalArgumentException("반려 의견은 필수입니다.");
        OwnerProfile owner = owner(id);
        Member reviewer = reviewer(reviewerLogin);
        owner.setReviewStatusCode(status);
        owner.setUpdatedAt(LocalDateTime.now());
        owners.save(owner);
        OwnerReviewHistory history = new OwnerReviewHistory();
        history.setOwnerId(id);
        history.setReviewerId(reviewer.getMemberId());
        history.setReviewStatusCode(status);
        history.setReviewComment(comment);
        history.setReviewedAt(LocalDateTime.now());
        history.setCreatedAt(LocalDateTime.now());
        reviews.save(history);
    }

    @Transactional
    public void suspendMember(Long id, String reasonCode, String detail, String reviewerLogin) {
        Member member = member(id);
        if ("ADMIN".equals(member.getRoleCode())) throw new IllegalArgumentException("관리자 계정은 정지할 수 없습니다.");
        if (blank(reasonCode)) throw new IllegalArgumentException("정지 사유 코드는 필수입니다.");
        if ("WITHDRAWN".equals(member.getStatus())) throw new IllegalStateException("탈퇴 회원은 정지할 수 없습니다.");
        MemberSuspensionHistory h = new MemberSuspensionHistory();
        h.setMemberId(id);
        h.setReasonCode(reasonCode);
        h.setReasonDetail(detail);
        h.setStartedAt(LocalDateTime.now());
        h.setProcessedBy(reviewer(reviewerLogin).getMemberId());
        h.setCreatedAt(LocalDateTime.now());
        suspensions.save(h);
        member.setStatus("SUSPENDED");
        member.setUpdatedAt(LocalDateTime.now());
        members.save(member);
    }

    @Transactional
    public void releaseMember(Long id) {
        Member member = member(id);
        if (!"SUSPENDED".equals(member.getStatus())) throw new IllegalStateException("정지 상태인 회원만 해제할 수 있습니다.");
        List<MemberSuspensionHistory> list = suspensions.findByMemberIdOrderByCreatedAtDesc(id);
        list.stream().filter(x -> x.getReleasedAt() == null).findFirst().ifPresent(x -> {
            x.setReleasedAt(LocalDateTime.now());
            suspensions.save(x);
        });
        member.setStatus("ACTIVE");
        member.setUpdatedAt(LocalDateTime.now());
        members.save(member);
    }

    @Transactional
    public void withdrawMember(Long id) {
        String login = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        withdrawMember(id, "관리자 강제 탈퇴", login);
    }

    @Transactional
    public void withdrawMember(Long id, String reason, String adminLogin) {
        Member member = member(id);
        if ("ADMIN".equals(member.getRoleCode())) throw new IllegalArgumentException("관리자 계정은 강제 탈퇴할 수 없습니다.");
        if ("WITHDRAWN".equals(member.getStatus())) return;
        MemberSuspensionHistory h = new MemberSuspensionHistory();
        h.setMemberId(id);
        h.setReasonCode("OTHER");
        h.setReasonDetail("강제 탈퇴: " + (blank(reason) ? "관리자 처리" : reason));
        h.setStartedAt(LocalDateTime.now());
        h.setProcessedBy(reviewer(adminLogin).getMemberId());
        h.setCreatedAt(LocalDateTime.now());
        suspensions.save(h);
        member.setStatus("WITHDRAWN");
        member.setDeletedAt(LocalDateTime.now());
        member.setUpdatedAt(LocalDateTime.now());
        members.save(member);
    }

    @Transactional
    public void setLodgingStatus(Long id, String status, String reason) {
        Lodging lodging = lodging(id);
        String previous = lodging.getStatus();
        boolean valid = ("PENDING".equals(previous) && Set.of("ACTIVE", "REJECTED").contains(status))
            || ("ACTIVE".equals(previous) && "SUSPENDED".equals(status))
            || ("SUSPENDED".equals(previous) && "ACTIVE".equals(status));
        if (!valid) throw new IllegalStateException("현재 상태에서 선택한 숙소 상태로 변경할 수 없습니다.");
        if ("REJECTED".equals(status) && blank(reason)) throw new IllegalArgumentException("반려 사유는 필수입니다.");
        if ("ACTIVE".equals(status) && "PENDING".equals(previous) && lodging.getOwnerId() != null
            && !"APPROVED".equals(owner(lodging.getOwnerId()).getReviewStatusCode()))
            throw new IllegalStateException("업주 가입 승인 후 숙소를 승인할 수 있습니다.");
        lodging.setStatus(status);
        lodging.setRejectReason("REJECTED".equals(status) ? reason : null);
        if ("ACTIVE".equals(status) && lodging.getApprovedAt() == null) lodging.setApprovedAt(LocalDateTime.now());
        lodging.setUpdatedAt(LocalDateTime.now());
        lodgings.save(lodging);
    }

    @Transactional
    public void updateLodging(Long id, String name, String address, String description) {
        Lodging lodging = lodging(id);
        if (blank(name)) throw new IllegalArgumentException("숙소명은 필수입니다.");
        lodging.setName(name.trim());
        if (!blank(address)) lodging.setAddress(address.trim());
        lodging.setDescription(description);
        lodging.setUpdatedAt(LocalDateTime.now());
        lodgings.save(lodging);
    }

    @Transactional
    public void setReservationStatus(Long id, String status, String reason, String adminLogin) {
        Reservation order = reservation(id);
        String current = order.getStatus();
        if ("CONFIRMED".equals(status)) {
            if (!"PENDING_PAYMENT".equals(current)) throw new IllegalStateException("결제 대기 예약만 확정할 수 있습니다.");
            List<Payment> done = paymentsForReservation(id).stream()
                .filter(p -> "DONE".equals(p.getStatus()))
                .filter(p -> Objects.equals(p.getAmount(), order.getTotalPrice()))
                .toList();
            if (done.isEmpty()) throw new IllegalStateException("결제 최종 승인과 금액이 확인되지 않았습니다.");
            int nights = (int) java.time.temporal.ChronoUnit.DAYS.between(order.getCheckinDate(), order.getCheckoutDate());
            if (nights < 1) throw new IllegalStateException("체크인·아웃 날짜가 올바르지 않습니다.");
            Integer holds = jdbc.queryForObject(
                "SELECT COUNT(*) FROM room_inventory WHERE room_id=? AND stay_date>=? AND stay_date<? AND reservation_id=? AND status='HELD' AND (hold_expires_at IS NULL OR hold_expires_at>NOW())",
                Integer.class, order.getRoomId(), order.getCheckinDate(), order.getCheckoutDate(), id);
            if (holds == null || holds != nights) throw new IllegalStateException("유효한 객실 임시 점유를 확인할 수 없습니다.");
            jdbc.update("UPDATE room_inventory SET status='BOOKED', hold_expires_at=NULL WHERE room_id=? AND stay_date>=? AND stay_date<? AND reservation_id=?",
                order.getRoomId(), order.getCheckinDate(), order.getCheckoutDate(), id);
        } else if ("CANCELLED".equals(status)) {
            if (!"PENDING_PAYMENT".equals(current)) throw new IllegalStateException("결제 대기 예약만 이 화면에서 취소할 수 있습니다.");
            if (paymentsForReservation(id).stream().anyMatch(p -> "DONE".equals(p.getStatus())))
                throw new IllegalStateException("승인된 결제가 있어 단순 미결제 취소로 처리할 수 없습니다.");
            jdbc.update("UPDATE room_inventory SET status='AVAILABLE', reservation_id=NULL, hold_expires_at=NULL WHERE reservation_id=? AND status='HELD'", id);
            order.setCancelledAt(LocalDateTime.now());
        } else if ("COMPLETED".equals(status)) {
            if (!"CONFIRMED".equals(current)) throw new IllegalStateException("확정 예약만 숙박 완료할 수 있습니다.");
        } else if ("REFUNDING".equals(status)) {
            if (!"CONFIRMED".equals(current)) throw new IllegalStateException("확정 예약만 환불 처리할 수 있습니다.");
            paymentsForReservation(id).stream()
                .filter(p -> "DONE".equals(p.getStatus()))
                .findFirst().orElseThrow(() -> new IllegalStateException("승인된 결제 내역이 없습니다."));
        } else if ("REFUND_FAILED".equals(status)) {
            if (!"REFUNDING".equals(current)) throw new IllegalStateException("환불 처리 중인 예약만 실패로 표시할 수 있습니다.");
        } else throw new IllegalArgumentException("지원하지 않는 예약 상태입니다.");
        order.setStatus(status);
        if ("CANCELLED".equals(status)) order.setCancelReason(reason);
        order.setUpdatedAt(LocalDateTime.now());
        reservations.saveAndFlush(order);
        ReservationActionHistory history = new ReservationActionHistory();
        history.setReservationId(id);
        history.setAdminId(reviewer(adminLogin).getMemberId());
        history.setBeforeStatus(current);
        history.setAfterStatus(status);
        history.setReason(reason);
        history.setCreatedAt(LocalDateTime.now());
        reservationActions.save(history);
    }

    @Transactional
    public void setHumanCourseStatus(Long id, String status, String login, String reason) {
        if (!Set.of("VISIBLE", "BLINDED", "DELETED").contains(status)) throw new IllegalArgumentException("지원하지 않는 코스 상태입니다.");
        HumanCourse course = humanCourse(id);
        course.setStatus(status);
        if ("DELETED".equals(status)) course.setFeatured(false);
        humanCourses.save(course);
        recordHumanAction(id, null, login, status, reason);
    }

    @Transactional
    public void setHumanFeatured(Long id, boolean featured, String login) {
        HumanCourse course = humanCourse(id);
        if (featured && !"VISIBLE".equals(course.getStatus())) throw new IllegalStateException("노출 중인 코스만 메인에 지정할 수 있습니다.");
        course.setFeatured(featured);
        humanCourses.save(course);
        recordHumanAction(id, null, login, featured ? "FEATURE" : "UNFEATURE", null);
    }

    @Transactional
    public void resolveHumanReport(Long id, String status, String login, String reason) {
        if (!Set.of("RESOLVED", "DISMISSED").contains(status)) throw new IllegalArgumentException("신고 처리 상태가 올바르지 않습니다.");
        HumanReport report = humanReports.findById(id).orElseThrow(() -> missing("신고"));
        report.setStatus(status);
        report.setResolvedAt(LocalDateTime.now());
        humanReports.save(report);
        recordHumanAction(report.getCourseId(), id, login, status, reason);
    }

    @Transactional
    public void updateCriterion(Long id, String value) {
        if (blank(value)) throw new IllegalArgumentException("설정값은 필수입니다.");
        RecommendationCriteria criterion = criteria.findById(id).orElseThrow(() -> missing("추천 기준"));
        RecommendationCriteriaHistory history = new RecommendationCriteriaHistory();
        history.setCriteriaId(id);
        String login = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        history.setAdminId(reviewer(login).getMemberId());
        history.setOldValue(criterion.getValue());
        history.setNewValue(value.trim());
        history.setCreatedAt(LocalDateTime.now());
        criteriaHistory.save(history);
        criterion.setValue(value.trim());
        criterion.setUpdatedAt(LocalDateTime.now());
        criteria.save(criterion);
    }

    private void recordHumanAction(Long courseId, Long reportId, String login, String action, String reason) {
        HumanActionHistory h = new HumanActionHistory();
        h.setCourseId(courseId);
        h.setReportId(reportId);
        h.setAdminId(reviewer(login).getMemberId());
        h.setAction(action);
        h.setNote(reason);
        h.setCreatedAt(LocalDateTime.now());
        humanActions.save(h);
    }

    private Member reviewer(String login) {
        Member m = members.findByLoginId(login).orElseThrow(() -> missing("처리자"));
        if (!"ADMIN".equals(m.getRoleCode())) throw new IllegalStateException("관리자 권한이 필요합니다.");
        return m;
    }
    private static boolean blank(String s) { return s == null || s.isBlank(); }
    private static IllegalArgumentException missing(String item) { return new IllegalArgumentException(item + " 정보를 찾을 수 없습니다."); }
}
