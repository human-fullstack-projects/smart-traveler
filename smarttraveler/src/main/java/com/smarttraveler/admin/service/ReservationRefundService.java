package com.smarttraveler.admin.service;

import com.smarttraveler.payment.service.TossGateway;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * 확정 예약 취소·환불. 관리자는 기존 환불 규정을 따르고 회원 직접 취소는 테스트 결제 전액을 환불한다.
 * 순서: ① 예약을 「환불 처리 중」으로 잠금 → ② 토스 결제 취소(0원이면 생략) → ③ 성공 = 취소·객실 수량 되돌림 / 실패 = 「환불 실패」.
 * 새 표·칸 없이 room_payment(refund_amount·refunded_at·cancel_reason·status)와 기존 이력 표만 쓴다.
 */
@Service
public class ReservationRefundService {
    private static final Set<String> START = Set.of("CONFIRMED", "REFUND_FAILED");
    private static final long STUCK_MINUTES = 3;
    private static final String MEMBER_CANCEL_REASON = "회원 요청으로 예약 취소";

    private final JdbcTemplate jdbc;
    private final TossGateway toss;
    private final TransactionTemplate tx;
    private final ObjectMapper mapper;

    public ReservationRefundService(JdbcTemplate jdbc, TossGateway toss, PlatformTransactionManager txManager, ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.toss = toss;
        this.tx = new TransactionTemplate(txManager);
        this.mapper = mapper;
    }

    /** 환불 비율(%) — 오늘 기준 체크인까지 남은 날로 계산. 체크인 날짜가 지났으면 -1. */
    public static int refundRate(LocalDate today, LocalDate checkin) {
        long days = ChronoUnit.DAYS.between(today, checkin);
        if (days < 0) return -1;
        if (days >= 7) return 100;
        if (days >= 3) return 50;
        return 0;
    }

    /** 관리자 화면에 미리 보여 줄 환불 예정. 취소할 수 없는 상태면 cancellable=false. */
    public Map<String, Object> preview(Long reservationId) {
        Map<String, Object> r = jdbc.queryForList("SELECT status, checkin_date, total_price FROM room_reservation WHERE reservation_id=?", reservationId)
                .stream().findFirst().orElse(null);
        Map<String, Object> out = new LinkedHashMap<>();
        if (r == null) return out;
        String status = (String) r.get("status");
        LocalDate checkin = toDate(r.get("checkin_date"));
        Map<String, Object> pay = refundablePayment(reservationId);
        int rate = refundRate(LocalDate.now(), checkin);
        int amount = pay == null ? 0 : ((Number) pay.get("amount")).intValue();
        out.put("daysBefore", ChronoUnit.DAYS.between(LocalDate.now(), checkin));
        out.put("rate", Math.max(rate, 0));
        out.put("paidAmount", amount);
        Integer planned = plannedAmount(status, pay);
        out.put("refundAmount", planned != null ? planned : amount * Math.max(rate, 0) / 100);
        out.put("cancellable", (START.contains(status) || stuckRefunding(reservationId, status)) && pay != null && (rate >= 0 || planned != null));
        out.put("retry", "REFUND_FAILED".equals(status) || "REFUNDING".equals(status));
        return out;
    }

    /** 관리자 취소·환불 (확정 예약 또는 환불 실패 예약 다시 시도). 결과 예약 상태를 돌려준다. */
    public String cancelAndRefund(Long reservationId, String reason, String adminLogin) {
        String why = reason == null ? "" : reason.trim();
        if (why.isEmpty()) throw new IllegalArgumentException("취소 사유를 입력해 주세요.");
        if (why.length() > 200) throw new IllegalArgumentException("취소 사유는 200자까지 쓸 수 있어요.");
        Long adminId = jdbc.queryForList("SELECT member_id FROM members WHERE login_id=?", Long.class, adminLogin)
                .stream().findFirst().orElseThrow(() -> new IllegalStateException("관리자 계정을 찾을 수 없습니다."));
        return refund(reservationId, why, adminId, null);
    }

    public String cancelByMember(Long reservationId, String loginId) {
        Long memberId = jdbc.queryForList("SELECT member_id FROM members WHERE login_id=?", Long.class, loginId)
                .stream().findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        return refund(reservationId, MEMBER_CANCEL_REASON, null, memberId);
    }

    private String refund(Long reservationId, String why, Long adminId, Long memberId) {
        // ① 잠금: CONFIRMED·REFUND_FAILED(또는 오래 멈춘 REFUNDING) → REFUNDING
        Lock lock = tx.execute(s -> lockForRefund(reservationId, why, adminId, memberId));

        // ② 토스 결제 취소 (트랜잭션 밖 — 외부 호출 동안 DB 잠금을 잡지 않음)
        String failure = null;
        JsonNode result = null;
        if (lock.refundAmount > 0) {
            try {
                JsonNode now = toss.get(lock.paymentKey);
                if (alreadyCancelled(now, lock.refundAmount)) result = now;   // 앞선 시도가 토스에선 성공한 경우
                else result = toss.cancel(lock.paymentKey, why,
                        lock.refundAmount.equals(lock.paidAmount) ? null : lock.refundAmount,
                        memberId == null ? "refund-" + lock.paymentId + "-" + UUID.randomUUID() : "member-refund-" + lock.paymentId);
                if (!alreadyCancelled(result, lock.refundAmount)) {
                    failure = result == null ? "토스 취소 응답이 없습니다." : "토스 응답 상태: " + result.path("status").asText();
                }
            } catch (RestClientResponseException e) {
                failure = tossError(e);
            } catch (RestClientException | IllegalStateException e) {
                failure = "토스 연결 실패: " + e.getMessage();
            }
        }

        // ③ 마무리
        final String fail = failure;
        final JsonNode done = result;
        return tx.execute(s -> fail == null ? finishCancelled(reservationId, lock, done, why, adminId)
                : finishFailed(reservationId, lock, fail, adminId));
    }

    private record Lock(Long paymentId, String paymentKey, Integer paidAmount, Integer refundAmount,
                        int version) {}

    private Lock lockForRefund(Long id, String reason, Long adminId, Long memberId) {
        Map<String, Object> r = jdbc.queryForList("""
                SELECT status, version, checkin_date FROM room_reservation
                WHERE reservation_id=? AND member_id=COALESCE(?, member_id) FOR UPDATE""", id, memberId)
                .stream().findFirst().orElseThrow(() -> memberId == null
                        ? new IllegalArgumentException("예약을 찾을 수 없습니다.")
                        : new ResponseStatusException(HttpStatus.NOT_FOUND));
        String status = (String) r.get("status");
        if (!START.contains(status) && !stuckRefunding(id, status)) {
            if ("REFUNDING".equals(status)) throw new IllegalStateException("이미 환불 처리 중이에요. 잠시 뒤 새로고침해 주세요.");
            throw new IllegalStateException("확정 예약(또는 환불 실패 예약)만 취소·환불할 수 있어요.");
        }
        Map<String, Object> pay = refundablePayment(id);
        if (pay == null) throw new IllegalStateException("승인된 결제 내역이 없어 환불할 수 없어요.");
        int paid = ((Number) pay.get("amount")).intValue();
        Integer planned = plannedAmount(status, pay);
        int refund;
        if (memberId != null) {
            if (planned != null && planned != paid) throw new IllegalStateException("관리자 환불이 진행된 예약입니다. 관리자 화면에서 처리해야 합니다.");
            if (planned == null && toDate(r.get("checkin_date")).isBefore(LocalDate.now(ZoneId.of("Asia/Seoul")))) {
                throw new IllegalStateException("체크인 날짜가 지나 취소할 수 없어요.");
            }
            refund = paid;
        } else if (planned != null) refund = planned;
        else {
            int rate = refundRate(LocalDate.now(), toDate(r.get("checkin_date")));
            if (rate < 0) throw new IllegalStateException("체크인 날짜가 지나 취소할 수 없어요.");
            refund = paid * rate / 100;
        }
        Long paymentId = ((Number) pay.get("payment_id")).longValue();
        // 정한 환불 금액을 먼저 적어 둠(refunded_at이 비어 있으면 「예정」)
        jdbc.update("UPDATE room_payment SET refund_amount=?, cancel_reason=?, updated_at=NOW() WHERE payment_id=?", refund, reason, paymentId);
        int updated = jdbc.update("""
                UPDATE room_reservation SET status='REFUNDING', updated_at=NOW(), version=version+1
                WHERE reservation_id=? AND status=? AND version=?""", id, status, r.get("version"));
        if (updated != 1) throw new IllegalStateException("다른 처리로 예약 상태가 바뀌었어요. 새로고침 후 다시 해 주세요.");
        history(id, adminId, status, "REFUNDING", "환불 " + refund + "원 요청 · " + reason);
        return new Lock(paymentId, (String) pay.get("payment_key"), paid, refund, ((Number) r.get("version")).intValue() + 1);
    }

    private String finishCancelled(Long id, Lock lock, JsonNode result, String reason, Long adminId) {
        int updated = jdbc.update("""
                UPDATE room_reservation SET status='CANCELLED', cancel_reason=?, cancelled_at=NOW(), updated_at=NOW(), version=version+1
                WHERE reservation_id=? AND status='REFUNDING' AND version=?""", reason, id, lock.version);
        if (updated != 1) throw new IllegalStateException("예약 상태가 바뀌었어요. 새로고침 후 확인해 주세요.");
        LocalDateTime refundedAt = LocalDateTime.now();
        String payStatus = "DONE";
        if (result != null) {
            payStatus = result.path("status").asText("CANCELED");
            JsonNode cancels = result.path("cancels");
            if (cancels.isArray() && !cancels.isEmpty()) {
                String at = cancels.get(cancels.size() - 1).path("canceledAt").asText(null);
                if (at != null) refundedAt = OffsetDateTime.parse(at).toLocalDateTime();
            }
        }
        jdbc.update("UPDATE room_payment SET status=?, refunded_at=?, fail_reason=NULL, updated_at=NOW() WHERE payment_id=?",
                payStatus, refundedAt, lock.paymentId);
        // 객실 수량 되돌리기 (확정 = RESERVED 배정)
        int restored = jdbc.update("""
                UPDATE room_inventory i JOIN reservation_inventory a ON a.inventory_id=i.inventory_id
                SET i.reserved_quantity=i.reserved_quantity-a.quantity
                WHERE a.reservation_id=? AND a.allocation_status='RESERVED' AND i.reserved_quantity>=a.quantity""", id);
        int released = jdbc.update("UPDATE reservation_inventory SET allocation_status='CANCELLED', hold_expires_at=NULL, updated_at=NOW() WHERE reservation_id=? AND allocation_status='RESERVED'", id);
        if (restored != released) throw new IllegalStateException("객실 재고를 복구하지 못했어요. 잠시 뒤 다시 확인해 주세요.");
        history(id, adminId, "REFUNDING", "CANCELLED", lock.refundAmount > 0 ? "환불 완료 " + lock.refundAmount + "원" : "환불 0원 · 취소");
        return "CANCELLED";
    }

    private String finishFailed(Long id, Lock lock, String failure, Long adminId) {
        String msg = failure.length() > 480 ? failure.substring(0, 480) : failure;
        int updated = jdbc.update("""
                UPDATE room_reservation SET status='REFUND_FAILED', updated_at=NOW(), version=version+1
                WHERE reservation_id=? AND status='REFUNDING' AND version=?""", id, lock.version);
        if (updated != 1) throw new IllegalStateException("예약 상태가 바뀌었어요. 새로고침 후 확인해 주세요.");
        jdbc.update("UPDATE room_payment SET fail_reason=?, updated_at=NOW() WHERE payment_id=?", msg, lock.paymentId);
        history(id, adminId, "REFUNDING", "REFUND_FAILED", msg);
        return "REFUND_FAILED";
    }

    private void history(Long id, Long adminId, String before, String after, String reason) {
        if (adminId == null) return;
        String text = reason.length() > 250 ? reason.substring(0, 250) : reason;
        jdbc.update("""
                INSERT INTO admin_reservation_action_history (reservation_id, admin_id, before_status, after_status, reason, created_at)
                VALUES (?,?,?,?,?,NOW())""", id, adminId, before, after, text);
    }

    private Map<String, Object> refundablePayment(Long reservationId) {
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT payment_id, payment_key, amount, refund_amount, refunded_at FROM room_payment
                WHERE reservation_id=? AND payment_key IS NOT NULL
                  AND (status='DONE' OR (status IN ('CANCELED', 'PARTIAL_CANCELED') AND approved_at IS NOT NULL AND refunded_at IS NULL))
                ORDER BY payment_id DESC""", reservationId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    /** 다시 시도 때는 처음 정한 환불 금액을 쓴다. */
    private Integer plannedAmount(String status, Map<String, Object> pay) {
        if (pay == null || !Set.of("REFUND_FAILED", "REFUNDING").contains(status) || pay.get("refunded_at") != null) return null;
        return pay.get("refund_amount") == null ? null : ((Number) pay.get("refund_amount")).intValue();
    }

    /** 서버가 토스 호출 중 멈춰 REFUNDING에 남은 예약은 몇 분 뒤 다시 시도할 수 있게. */
    private boolean stuckRefunding(Long id, String status) {
        if (!"REFUNDING".equals(status)) return false;
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM room_reservation WHERE reservation_id=? AND updated_at < NOW() - INTERVAL ? MINUTE",
                Integer.class, id, STUCK_MINUTES);
        return n != null && n > 0;
    }

    private static boolean alreadyCancelled(JsonNode payment, int refund) {
        if (payment == null) return false;
        String st = payment.path("status").asText();
        if (!Set.of("CANCELED", "PARTIAL_CANCELED").contains(st)) return false;
        int cancelled = 0;
        for (JsonNode c : payment.path("cancels")) if ("DONE".equals(c.path("cancelStatus").asText("DONE"))) cancelled += c.path("cancelAmount").asInt();
        return cancelled >= refund;
    }

    private String tossError(RestClientResponseException e) {
        try {
            JsonNode body = mapper.readTree(e.getResponseBodyAsString());
            return "토스 " + e.getStatusCode().value() + " " + body.path("code").asText() + ": " + body.path("message").asText();
        } catch (RuntimeException parse) {
            return "토스 " + e.getStatusCode().value();
        }
    }

    private static LocalDate toDate(Object v) {
        if (v instanceof LocalDate d) return d;
        if (v instanceof java.sql.Date d) return d.toLocalDate();
        if (v instanceof LocalDateTime t) return t.toLocalDate();
        return LocalDate.parse(String.valueOf(v).substring(0, 10));
    }
}
