package com.smarttraveler.owner.service;

import com.smarttraveler.owner.dto.OwnerRoomDtos.InventoryDay;
import com.smarttraveler.owner.dto.OwnerRoomDtos.InventoryUpdate;
import com.smarttraveler.owner.dto.OwnerRoomDtos.OwnerReservation;
import com.smarttraveler.owner.dto.OwnerRoomDtos.OwnerRoom;
import com.smarttraveler.owner.dto.OwnerRoomDtos.TodaySummary;
import com.smarttraveler.owner.dto.OwnerRoomDtos.RoomInput;
import java.sql.Statement;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * 업주 객실·판매 수량·예약 (피드백: 객실이 어느 숙소 것인지 표기 · 숙소 등록 때 객실 입력 · 재고 0이면 객실 없음).
 * 판매 수량은 room_inventory(날짜별 total_quantity)에 저장한다. 객실을 만들면 오늘부터 90일 판매 칸을 함께 만든다.
 */
@Service
public class OwnerRoomService {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private final JdbcTemplate jdbc;
    private final OwnerLodgingService lodgings;
    private final OwnerRoomWriter writer;

    public OwnerRoomService(JdbcTemplate jdbc, OwnerLodgingService lodgings, OwnerRoomWriter writer) {
        this.jdbc = jdbc;
        this.lodgings = lodgings;
        this.writer = writer;
    }

    /** 내 숙소들의 객실 (숙소 이름 포함) + 오늘 판매 수량·남은 수량. */
    @Transactional(readOnly = true)
    public List<OwnerRoom> rooms(String loginId) {
        long ownerId = lodgings.ownerIdOf(loginId);
        return jdbc.query("""
            SELECT r.room_id, r.lodging_id, l.name AS lodging_name, r.name, r.base_capacity, r.max_capacity,
                   r.price_off_weekday, r.price_off_weekend, r.is_active,
                   i.total_quantity, (i.total_quantity - i.held_quantity - i.reserved_quantity) AS remaining
            FROM room r JOIN lodging l ON l.lodging_id = r.lodging_id
            LEFT JOIN room_inventory i ON i.room_id = r.room_id AND i.stay_date = ?
            WHERE l.owner_id = ?
            ORDER BY l.lodging_id, r.room_id
            """, (rs, row) -> new OwnerRoom(rs.getLong("room_id"), rs.getLong("lodging_id"), rs.getString("lodging_name"),
                rs.getString("name"), rs.getInt("base_capacity"), rs.getInt("max_capacity"),
                rs.getInt("price_off_weekday"), rs.getInt("price_off_weekend"), rs.getBoolean("is_active"),
                (Integer) rs.getObject("total_quantity", Integer.class), (Integer) rs.getObject("remaining", Integer.class)),
            LocalDate.now(KST), ownerId);
    }

    /** 내 숙소에 객실 1개 추가 (+90일 판매 칸). */
    @Transactional
    public OwnerRoom addRoom(String loginId, long lodgingId, RoomInput input) {
        long ownerId = lodgings.ownerIdOf(loginId);
        requireMyLodging(ownerId, lodgingId);
        long roomId = writer.insertRoom(lodgingId, input);
        return rooms(loginId).stream().filter(room -> room.roomId() == roomId).findFirst().orElseThrow();
    }

    /** 날짜별 판매 수량 (오늘부터 days일). */
    @Transactional(readOnly = true)
    public List<InventoryDay> inventory(String loginId, long roomId, LocalDate from, int days) {
        requireMyRoom(lodgings.ownerIdOf(loginId), roomId);
        LocalDate start = from == null ? LocalDate.now(KST) : from;
        int span = Math.max(1, Math.min(days, 92));
        return jdbc.query("""
            SELECT stay_date, total_quantity, reserved_quantity, held_quantity, is_on_sale, price
            FROM room_inventory WHERE room_id = ? AND stay_date >= ? AND stay_date < ?
            ORDER BY stay_date
            """, (rs, row) -> new InventoryDay(rs.getDate("stay_date").toLocalDate(), rs.getInt("total_quantity"),
                rs.getInt("reserved_quantity") + rs.getInt("held_quantity"),
                rs.getInt("total_quantity") - rs.getInt("reserved_quantity") - rs.getInt("held_quantity"),
                rs.getBoolean("is_on_sale"), rs.getInt("price")),
            roomId, start, start.plusDays(span));
    }

    /** 기간의 판매 수량을 정한다. 이미 예약·결제 중인 수보다 적게는 못 정한다. 판매 칸이 없는 날은 새로 만든다. */
    @Transactional
    public List<InventoryDay> setQuantity(String loginId, long roomId, InventoryUpdate update) {
        requireMyRoom(lodgings.ownerIdOf(loginId), roomId);
        if (update.to().isBefore(update.from()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "끝 날짜가 시작 날짜보다 빠릅니다.");
        if (update.from().plusDays(366).isBefore(update.to()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "한 번에 1년까지만 정할 수 있습니다.");
        var prices = jdbc.queryForMap("SELECT price_off_weekday, price_off_weekend FROM room WHERE room_id = ?", roomId);
        for (LocalDate day = update.from(); !day.isAfter(update.to()); day = day.plusDays(1)) {
            var used = jdbc.query("SELECT reserved_quantity + held_quantity FROM room_inventory WHERE room_id = ? AND stay_date = ? FOR UPDATE",
                (rs, row) -> rs.getInt(1), roomId, day);
            if (!used.isEmpty() && used.getFirst() > update.quantity())
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                    day + "에는 이미 예약된 객실이 " + used.getFirst() + "개 있어서 그보다 적게 정할 수 없습니다.");
            int price = OwnerRoomWriter.weekend(day) ? ((Number) prices.get("price_off_weekend")).intValue() : ((Number) prices.get("price_off_weekday")).intValue();
            jdbc.update("""
                INSERT INTO room_inventory (room_id, stay_date, total_quantity, held_quantity, reserved_quantity, is_on_sale, price, price_type)
                VALUES (?, ?, ?, 0, 0, TRUE, ?, 'OWNER_SET')
                ON DUPLICATE KEY UPDATE total_quantity = VALUES(total_quantity), updated_at = CURRENT_TIMESTAMP(6)
                """, roomId, day, update.quantity(), price);
        }
        return inventory(loginId, roomId, update.from(), (int) (update.to().toEpochDay() - update.from().toEpochDay()) + 1);
    }

    /** 내 숙소들의 예약 (최근 체크인 순). 상태 이름은 화면이 정한다(CONFIRMED = 「예약」). */
    @Transactional(readOnly = true)
    public List<OwnerReservation> reservations(String loginId) {
        long ownerId = lodgings.ownerIdOf(loginId);
        return jdbc.query("""
            SELECT rr.reservation_id, rr.reservation_no, rr.booker_name, rr.booker_phone, rr.booker_email,
                   COALESCE(rr.visitor_name, rr.booker_name) AS visitor_name, rr.guest_count, rr.room_quantity,
                   rr.checkin_date, rr.checkout_date, rr.total_price, rr.status, rr.request_memo,
                   r.name AS room_name, l.name AS lodging_name
            FROM room_reservation rr JOIN room r ON r.room_id = rr.room_id JOIN lodging l ON l.lodging_id = r.lodging_id
            WHERE l.owner_id = ? AND rr.status <> 'EXPIRED'
            ORDER BY rr.checkin_date DESC, rr.reservation_id DESC
            LIMIT 500
            """, (rs, row) -> new OwnerReservation(rs.getLong("reservation_id"), rs.getString("reservation_no"),
                rs.getString("booker_name"), rs.getString("booker_phone"), rs.getInt("guest_count"), rs.getInt("room_quantity"),
                rs.getDate("checkin_date").toLocalDate(), rs.getDate("checkout_date").toLocalDate(), rs.getInt("total_price"),
                rs.getString("status"), rs.getString("request_memo"), rs.getString("room_name"), rs.getString("lodging_name"),
                rs.getString("booker_email"), rs.getString("visitor_name")),
            ownerId);
    }

    /** 결제 완료(CONFIRMED)됐지만 업주가 아직 확인하지 않은 예약 수를 신규 예약으로 본다. */
    @Transactional(readOnly = true)
    public int newReservationCount(String loginId) {
        long ownerId = lodgings.ownerIdOf(loginId);
        return jdbc.queryForObject("""
            SELECT COUNT(*)
            FROM room_reservation rr JOIN room r ON r.room_id = rr.room_id JOIN lodging l ON l.lodging_id = r.lodging_id
            WHERE l.owner_id = ? AND rr.status = 'CONFIRMED' AND rr.owner_checked_at IS NULL
            """, Integer.class, ownerId);
    }

    /** 예약 현황 탭을 열면 호출 : 미확인 예약을 전부 확인 처리한다. */
    @Transactional
    public void markReservationsChecked(String loginId) {
        long ownerId = lodgings.ownerIdOf(loginId);
        jdbc.update("""
            UPDATE room_reservation rr JOIN room r ON r.room_id = rr.room_id JOIN lodging l ON l.lodging_id = r.lodging_id
            SET rr.owner_checked_at = NOW()
            WHERE l.owner_id = ? AND rr.status = 'CONFIRMED' AND rr.owner_checked_at IS NULL
            """, ownerId);
    }

    /** 오늘 체크인·체크아웃 건수 (취소·환불 제외). */
    @Transactional(readOnly = true)
    public TodaySummary todaySummary(String loginId) {
        long ownerId = lodgings.ownerIdOf(loginId);
        int checkIn = jdbc.queryForObject("""
            SELECT COUNT(*)
            FROM room_reservation rr JOIN room r ON r.room_id = rr.room_id JOIN lodging l ON l.lodging_id = r.lodging_id
            WHERE l.owner_id = ? AND rr.checkin_date = CURRENT_DATE()
              AND rr.status NOT IN ('CANCELLED', 'CANCELED', 'REFUNDING', 'REFUNDED', 'REFUND_FAILED')
            """, Integer.class, ownerId);
        int checkOut = jdbc.queryForObject("""
            SELECT COUNT(*)
            FROM room_reservation rr JOIN room r ON r.room_id = rr.room_id JOIN lodging l ON l.lodging_id = r.lodging_id
            WHERE l.owner_id = ? AND rr.checkout_date = CURRENT_DATE()
              AND rr.status NOT IN ('CANCELLED', 'CANCELED', 'REFUNDING', 'REFUNDED', 'REFUND_FAILED')
            """, Integer.class, ownerId);
        return new TodaySummary(checkIn, checkOut);
    }

    private void requireMyLodging(long ownerId, long lodgingId) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM lodging WHERE lodging_id = ? AND owner_id = ?", Integer.class, lodgingId, ownerId);
        if (count == null || count == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "숙소를 찾을 수 없습니다.");
    }

    private void requireMyRoom(long ownerId, long roomId) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM room r JOIN lodging l ON l.lodging_id = r.lodging_id WHERE r.room_id = ? AND l.owner_id = ?",
            Integer.class, roomId, ownerId);
        if (count == null || count == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "객실을 찾을 수 없습니다.");
    }
}
