package com.smarttraveler.payment.service;

import com.smarttraveler.auth.repository.MemberRepository;
import com.smarttraveler.domain.entity.RoomPayment;
import com.smarttraveler.domain.entity.RoomReservation;
import com.smarttraveler.payment.dto.CheckoutResponse;
import com.smarttraveler.payment.repository.PaymentRepository;
import com.smarttraveler.reservation.repository.ReservationRepository;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.JsonNode;

@Service
public class TossPaymentService {
    private final PaymentRepository payments;
    private final ReservationRepository reservations;
    private final MemberRepository members;
    private final JdbcTemplate jdbc;
    private final TossGateway toss;
    private final TossWebhookVerifier webhookVerifier;
    private final ObjectMapper mapper;
    private final String clientKey;

    public TossPaymentService(PaymentRepository payments, ReservationRepository reservations,
                              MemberRepository members, JdbcTemplate jdbc, TossGateway toss,
                              TossWebhookVerifier webhookVerifier, ObjectMapper mapper,
                              @Value("${app.toss.client-key:}") String clientKey) {
        this.payments = payments;
        this.reservations = reservations;
        this.members = members;
        this.jdbc = jdbc;
        this.toss = toss;
        this.webhookVerifier = webhookVerifier;
        this.mapper = mapper;
        this.clientKey = clientKey;
    }

    @Transactional
    public CheckoutResponse prepare(Long reservationId, Authentication auth) {
        RoomReservation reservation = ownedReservation(reservationId, auth);
        lockReservation(reservationId);
        if (!clientKey.startsWith("test_gck_")) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "토스 테스트 키를 설정해 주세요.");
        }
        requireHeld(reservation);
        RoomPayment payment = payments.findFirstByReservationIdOrderByCreatedAtDesc(reservationId)
                .orElseGet(() -> payments.save(newPayment(reservation)));
        if (!Objects.equals(payment.getAmount(), reservation.getTotalPrice())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "예약 금액과 결제 금액이 다릅니다.");
        }
        return checkout(reservation, payment);
    }

    @Transactional
    public CheckoutResponse confirm(String orderId, String paymentKey, int amount, Authentication auth) {
        RoomPayment payment = payments.findByOrderId(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "결제 주문을 찾을 수 없습니다."));
        RoomReservation reservation = ownedReservation(payment.getReservationId(), auth);
        lockReservation(reservation.getReservationId());
        if (amount <= 0 || amount != payment.getAmount() || amount != reservation.getTotalPrice()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "결제 금액이 일치하지 않습니다.");
        }
        if (paymentKey == null || paymentKey.isBlank() || paymentKey.length() > 200) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "결제 키가 올바르지 않습니다.");
        }
        if ("DONE".equals(payment.getStatus())) return checkout(reservation, payment);
        requireHeld(reservation);
        if (payment.getPaymentKey() != null && !paymentKey.equals(payment.getPaymentKey())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "다른 결제 키로 이미 진행 중입니다.");
        }
        payment.setPaymentKey(paymentKey);
        payment.setStatus("IN_PROGRESS");
        payments.saveAndFlush(payment);
        JsonNode result;
        try {
            result = toss.confirm(paymentKey, orderId, amount, payment.getIdempotencyKey());
        } catch (RestClientException error) {
            // 승인 응답이 유실될 수 있으므로 PG 조회로 결과를 확인한다.
            try { result = toss.get(paymentKey); }
            catch (RestClientException lookupError) { return checkout(reservation, payment); }
        }
        applyResult(reservation, payment, result);
        return checkout(reservation, payment);
    }

    @Transactional
    public CheckoutResponse sync(Long reservationId, Authentication auth) {
        RoomReservation reservation = ownedReservation(reservationId, auth);
        lockReservation(reservationId);
        RoomPayment payment = payments.findFirstByReservationIdOrderByCreatedAtDesc(reservationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "결제 요청을 찾을 수 없습니다."));
        if (payment.getPaymentKey() != null) applyResult(reservation, payment, toss.get(payment.getPaymentKey()));
        return checkout(reservation, payment);
    }

    @Transactional
    public void webhook(String payload, String transmissionTime, String signature) {
        webhookVerifier.verify(payload, transmissionTime, signature);
        JsonNode event = mapper.readTree(payload);
        if (!"PAYMENT_STATUS_CHANGED".equals(event.path("eventType").asText())) return;
        JsonNode data = event.path("data");
        String orderId = data.path("orderId").asText();
        String paymentKey = data.path("paymentKey").asText();
        if (orderId.isBlank() || paymentKey.isBlank()) return;
        RoomPayment payment = payments.findByOrderId(orderId).orElse(null);
        if (payment == null || !paymentKey.equals(payment.getPaymentKey())) return;
        lockReservation(payment.getReservationId());
        RoomReservation reservation = reservations.findById(payment.getReservationId()).orElseThrow();
        applyResult(reservation, payment, toss.get(paymentKey));
    }

    private void applyResult(RoomReservation reservation, RoomPayment payment, JsonNode result) {
        if (result == null || !payment.getOrderId().equals(result.path("orderId").asText())
                || !Objects.equals(payment.getPaymentKey(), result.path("paymentKey").asText())
                || payment.getAmount() != result.path("totalAmount").asInt(-1)) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "토스 응답의 주문 정보가 일치하지 않습니다.");
        }
        String status = result.path("status").asText();
        payment.setStatus(status);
        payment.setMethod(result.path("method").asText(null));
        payment.setReceiptUrl(result.path("receipt").path("url").asText(null));
        if ("DONE".equals(status)) {
            String approvedAt = result.path("approvedAt").asText(null);
            if (approvedAt != null) payment.setApprovedAt(OffsetDateTime.parse(approvedAt).toLocalDateTime());
            long nights = ChronoUnit.DAYS.between(reservation.getCheckinDate(), reservation.getCheckoutDate());
            if ("PENDING_PAYMENT".equals(reservation.getStatus()) && nights > 0 && heldNights(reservation) == nights) {
                int updated = jdbc.update("""
                    UPDATE room_inventory i JOIN reservation_inventory a ON a.inventory_id=i.inventory_id
                    SET i.held_quantity=i.held_quantity-a.quantity, i.reserved_quantity=i.reserved_quantity+a.quantity
                    WHERE a.reservation_id=? AND a.allocation_status='HELD' AND i.held_quantity>=a.quantity
                    """, reservation.getReservationId());
                if (updated != nights) throw new ResponseStatusException(HttpStatus.CONFLICT, "객실 재고 상태가 변경됐습니다. 관리자 확인이 필요합니다.");
                jdbc.update("UPDATE reservation_inventory SET allocation_status='RESERVED', hold_expires_at=NULL WHERE reservation_id=? AND allocation_status='HELD'", reservation.getReservationId());
                reservation.setStatus("CONFIRMED");
                reservations.save(reservation);
            }
        }
        payments.save(payment);
    }

    private long heldNights(RoomReservation reservation) {
        Long count = jdbc.queryForObject("""
            SELECT COUNT(*) FROM reservation_inventory a JOIN room_inventory i ON i.inventory_id=a.inventory_id
            WHERE a.reservation_id=? AND a.room_id=? AND a.allocation_status='HELD'
              AND a.hold_expires_at>NOW() AND i.is_on_sale=TRUE AND i.held_quantity>=a.quantity
            """, Long.class, reservation.getReservationId(), reservation.getRoomId());
        return count == null ? 0 : count;
    }

    private void requireHeld(RoomReservation reservation) {
        long nights = ChronoUnit.DAYS.between(reservation.getCheckinDate(), reservation.getCheckoutDate());
        if (!"PENDING_PAYMENT".equals(reservation.getStatus()) || nights <= 0 || heldNights(reservation) != nights) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "유효한 결제 대기 객실 점유가 없습니다.");
        }
    }

    private void lockReservation(Long id) {
        jdbc.queryForObject("SELECT reservation_id FROM room_reservation WHERE reservation_id=? FOR UPDATE", Long.class, id);
    }

    private RoomReservation ownedReservation(Long id, Authentication auth) {
        Long memberId = members.findByLoginId(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED)).getMemberId();
        RoomReservation reservation = reservations.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "예약을 찾을 수 없습니다."));
        if (!memberId.equals(reservation.getMemberId())) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "예약을 찾을 수 없습니다.");
        return reservation;
    }

    private RoomPayment newPayment(RoomReservation reservation) {
        RoomPayment payment = new RoomPayment();
        payment.setReservationId(reservation.getReservationId());
        payment.setIdempotencyKey(UUID.randomUUID().toString());
        payment.setPgProvider("TOSS");
        payment.setOrderId(reservation.getReservationNo());
        payment.setOrderName("숙소 예약 " + reservation.getReservationNo());
        payment.setAmount(reservation.getTotalPrice());
        payment.setStatus("READY");
        payment.setRefundAmount(0);
        payment.setPayerMemberId(reservation.getMemberId());
        payment.setPayerName(reservation.getBookerName());
        payment.setPayerPhone(reservation.getBookerPhone());
        payment.setCreatedAt(LocalDateTime.now());
        payment.setUpdatedAt(LocalDateTime.now());
        return payment;
    }

    private CheckoutResponse checkout(RoomReservation reservation, RoomPayment payment) {
        return new CheckoutResponse(reservation.getReservationId(), payment.getOrderId(), payment.getOrderName(),
                payment.getAmount(), payment.getStatus(), reservation.getStatus(), clientKey, payment.getReceiptUrl());
    }
}
