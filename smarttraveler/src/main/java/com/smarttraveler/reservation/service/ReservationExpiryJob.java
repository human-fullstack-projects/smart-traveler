package com.smarttraveler.reservation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
@RequiredArgsConstructor
public class ReservationExpiryJob {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;

    @Scheduled(fixedDelay = 30000)
    public void releaseExpired() {
        var ids = jdbc.queryForList("""
            SELECT r.reservation_id FROM room_reservation r
            WHERE r.status='PENDING_PAYMENT'
              AND EXISTS (SELECT 1 FROM reservation_inventory a WHERE a.reservation_id=r.reservation_id
                          AND a.allocation_status='HELD' AND a.hold_expires_at<=NOW())
              AND NOT EXISTS (SELECT 1 FROM room_payment p WHERE p.reservation_id=r.reservation_id
                              AND p.status IN ('DONE', 'IN_PROGRESS', 'WAITING_FOR_DEPOSIT'))
            ORDER BY r.reservation_id LIMIT 50
            """, Long.class);
        for (Long id : ids) {
            transactions.executeWithoutResult(status -> release(id));
        }
    }

    private void release(Long id) {
        String status = jdbc.queryForObject("""
            SELECT status FROM room_reservation WHERE reservation_id=? FOR UPDATE
            """, String.class, id);
        if (!"PENDING_PAYMENT".equals(status)) return;

        int paymentCount = jdbc.queryForObject("""
            SELECT COUNT(*) FROM room_payment WHERE reservation_id=?
              AND status IN ('DONE', 'IN_PROGRESS', 'WAITING_FOR_DEPOSIT')
            """, Integer.class, id);
        if (paymentCount > 0) return;

        jdbc.update("""
            UPDATE room_inventory i JOIN reservation_inventory a ON a.inventory_id=i.inventory_id
            SET i.held_quantity=i.held_quantity-a.quantity
            WHERE a.reservation_id=? AND a.allocation_status='HELD'
            """, id);
        jdbc.update("""
            UPDATE reservation_inventory SET allocation_status='CANCELLED', hold_expires_at=NULL
            WHERE reservation_id=? AND allocation_status='HELD'
            """, id);
        jdbc.update("""
            UPDATE room_reservation
            SET status='CANCELLED', cancel_reason='결제 시간 초과', cancelled_at=NOW(), version=version+1
            WHERE reservation_id=?
            """, id);
    }
}
