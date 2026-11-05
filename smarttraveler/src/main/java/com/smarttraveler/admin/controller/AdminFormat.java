package com.smarttraveler.admin.controller;

import com.smarttraveler.domain.entity.OwnerProfile;
import com.smarttraveler.admin.repository.OwnerProfileRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Thymeleaf 표시용 변환: ${@fmt.label(code)}, ${@fmt.dt(time)}, ${@fmt.method(id)}, ${@fmt.owner(id)}. DB 값은 그대로 둔다. */
@Component("fmt")
public class AdminFormat {
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final Map<String, String> LABELS = Map.ofEntries(
            // 회원·계정
            Map.entry("ACTIVE", "정상"), Map.entry("SUSPENDED", "정지"), Map.entry("WITHDRAWN", "탈퇴"), Map.entry("DELETED", "탈퇴"),
            Map.entry("ADMIN", "관리자"), Map.entry("USER", "사용자"), Map.entry("OWNER", "업주"),
            // 업주 심사·숙소 승인
            Map.entry("PENDING", "승인대기"), Map.entry("APPROVED", "승인"), Map.entry("REJECTED", "반려"),
            // 예약·주문
            Map.entry("PENDING_PAYMENT", "결제대기"), Map.entry("CONFIRMED", "예약확정"), Map.entry("COMPLETED", "숙박완료"),
            Map.entry("REFUNDING", "환불처리중"), Map.entry("REFUND_FAILED", "환불실패"), Map.entry("REFUNDED", "환불완료"),
            Map.entry("CANCELLED", "취소"), Map.entry("EXPIRED", "만료"),
            // 결제
            Map.entry("READY", "결제 준비"), Map.entry("DONE", "결제 완료"), Map.entry("ABORTED", "결제 실패"), Map.entry("CANCELED", "결제 취소"),
            // 휴먼 추천 코스·신고
            Map.entry("VISIBLE", "게시 중"), Map.entry("BLINDED", "블라인드"), Map.entry("RESOLVED", "처리 완료"), Map.entry("DISMISSED", "기각"),
            Map.entry("RECEIVED", "신고 접수"), Map.entry("OPEN", "신고 접수"),
            Map.entry("FEATURE", "메인 노출"), Map.entry("UNFEATURE", "노출 해제"),
            // 정지 사유 (suspension_reason_codes 와 같은 이름)
            Map.entry("FRAUD", "부정 이용"), Map.entry("POLICY", "운영 정책 위반"), Map.entry("POLICY_VIOLATION", "운영 정책 위반"),
            Map.entry("OTHER", "기타"),
            // 관광지 데이터 수집 (data_sync_log)
            Map.entry("SUCCESS", "반영"), Map.entry("FAILED", "미반영"), Map.entry("SKIPPED", "건너뜀"),
            Map.entry("KTO_TOURAPI", "관광공사 관광정보"), Map.entry("KTO_HUB", "중심 관광지"), Map.entry("KTO_RELATED", "연관 관광지"),
            Map.entry("ATTRACTION", "관광지 정보"), Map.entry("HUB_RANK", "중심 순위"), Map.entry("RELATED", "연관 순위"),
            // AI 코스 생성
            Map.entry("done", "생성 완료"), Map.entry("failed", "생성 실패"), Map.entry("running", "생성 중"), Map.entry("pending", "대기"),
            Map.entry("preview", "미리보기"), Map.entry("selected", "선택됨"), Map.entry("confirmed", "확정"));

    private static final Map<Integer, String> METHODS = Map.of(1, "AI 생성", 2, "사용자 작성", 3, "AI 수정", 4, "템플릿");

    private final OwnerProfileRepository owners;

    public AdminFormat(OwnerProfileRepository owners) {
        this.owners = owners;
    }

    public String label(Object code) {
        if (code == null) return "-";
        String key = code.toString();
        return LABELS.getOrDefault(key, key);
    }

    public String dt(Object value) {
        if (value == null) return "-";
        if (value instanceof LocalDateTime t) return DATE_TIME.format(t);
        if (value instanceof LocalDate d) return DATE.format(d);
        if (value instanceof java.sql.Timestamp ts) return DATE_TIME.format(ts.toLocalDateTime());
        if (value instanceof TemporalAccessor t) return DATE_TIME.format(t);
        return value.toString();
    }

    public String method(Object id) {
        if (id == null) return "-";
        try {
            return METHODS.getOrDefault(Integer.parseInt(id.toString()), id.toString());
        } catch (NumberFormatException e) {
            return id.toString();
        }
    }

    /** 숙소의 업주 번호 → 상호명. owner_profiles.owner_id 또는 member_id 어느 쪽이든 찾는다. */
    public String owner(Long id) {
        if (id == null) return "-";
        return owners.findById(id).or(() -> owners.findByMemberId(id))
                .map(OwnerProfile::getBusinessName).orElse("업주 " + id);
    }
}
