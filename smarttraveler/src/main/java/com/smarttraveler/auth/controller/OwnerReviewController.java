package com.smarttraveler.auth.controller;

import com.smarttraveler.auth.repository.MemberRepository;
import com.smarttraveler.auth.repository.OwnerReviewRepository;
import com.smarttraveler.auth.repository.SignupOwnerRepository;
import com.smarttraveler.domain.entity.OwnerProfile;
import com.smarttraveler.domain.entity.OwnerReviewHistory;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth/owner-review")
public class OwnerReviewController {
    private final MemberRepository members;
    private final SignupOwnerRepository owners;
    private final OwnerReviewRepository reviews;

    public OwnerReviewController(MemberRepository members, SignupOwnerRepository owners, OwnerReviewRepository reviews) {
        this.members = members; this.owners = owners; this.reviews = reviews;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ReviewView mine(Principal principal) {
        return view(owner(principal));
    }

    @PostMapping("/reapply")
    @Transactional
    public ReviewView reapply(Principal principal, @RequestBody ReapplyRequest body) {
        OwnerProfile owner = owner(principal);
        if (!"REJECTED".equals(owner.getReviewStatusCode()))
            throw new IllegalStateException("반려된 신청만 다시 제출할 수 있습니다.");
        if (blank(body.businessNumber()) || blank(body.businessName()) || blank(body.representativeName()))
            throw new IllegalArgumentException("사업자번호, 상호명, 대표자명을 입력해주세요.");
        if (owners.existsByBusinessNumberAndOwnerIdNot(body.businessNumber().trim(), owner.getOwnerId()))
            throw new IllegalArgumentException("이미 등록된 사업자번호입니다.");
        owner.setBusinessNumber(body.businessNumber().trim());
        owner.setBusinessName(body.businessName().trim());
        owner.setRepresentativeName(body.representativeName().trim());
        owner.setBusinessAddress(body.businessAddress() == null ? null : body.businessAddress().trim());
        owner.setBusinessPhone(body.businessPhone() == null ? null : body.businessPhone().trim());
        owner.setReviewStatusCode("PENDING");
        owner.setUpdatedAt(LocalDateTime.now());
        owners.save(owner);
        OwnerReviewHistory history = new OwnerReviewHistory();
        history.setOwnerId(owner.getOwnerId());
        history.setReviewStatusCode("PENDING");
        history.setCreatedAt(LocalDateTime.now());
        reviews.save(history);
        return view(owner);
    }

    private OwnerProfile owner(Principal principal) {
        var member = members.findByLoginId(principal.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if (!"OWNER".equals(member.getRoleCode())) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return owners.findByMemberId(member.getMemberId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "업주 신청을 찾을 수 없습니다."));
    }

    private ReviewView view(OwnerProfile owner) {
        List<HistoryView> history = reviews.findByOwnerIdOrderByCreatedAtDesc(owner.getOwnerId()).stream()
                .map(h -> new HistoryView(h.getReviewStatusCode(), h.getReviewComment(), h.getReviewedAt(), h.getCreatedAt()))
                .toList();
        return new ReviewView(owner.getReviewStatusCode(), owner.getBusinessNumber(), owner.getBusinessName(),
                owner.getRepresentativeName(), owner.getBusinessAddress(), owner.getBusinessPhone(), history);
    }

    private static boolean blank(String value) { return value == null || value.isBlank(); }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public org.springframework.http.ResponseEntity<Map<String, String>> invalid(RuntimeException error) {
        return org.springframework.http.ResponseEntity.badRequest().body(Map.of("message", error.getMessage()));
    }

    public record ReapplyRequest(String businessNumber, String businessName, String representativeName,
                                 String businessAddress, String businessPhone) {}
    public record HistoryView(String status, String comment, LocalDateTime reviewedAt, LocalDateTime createdAt) {}
    public record ReviewView(String status, String businessNumber, String businessName, String representativeName,
                             String businessAddress, String businessPhone, List<HistoryView> history) {}
}
