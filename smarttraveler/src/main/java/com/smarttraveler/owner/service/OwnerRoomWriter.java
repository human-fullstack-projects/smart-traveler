package com.smarttraveler.owner.service;

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
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** 객실 + 오늘부터 90일 판매 칸(room_inventory)을 만든다. 숙소 등록과 객실 추가가 함께 쓴다. */
@Component
public class OwnerRoomWriter {
    static final int SALE_DAYS = 90;
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private final JdbcTemplate jdbc;

    public OwnerRoomWriter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** 숙소 등록 때 함께 받은 객실들 (호출한 쪽 트랜잭션 안에서). */
    public void insertRooms(long lodgingId, List<RoomInput> inputs) {
        if (inputs == null) return;
        for (RoomInput input : inputs) insertRoom(lodgingId, input);
    }

    public long insertRoom(long lodgingId, RoomInput input) {
        if (input.maxCapacity() < input.baseCapacity())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "최대 인원은 기준 인원보다 적을 수 없습니다.");
        var key = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            var statement = connection.prepareStatement("""
                INSERT INTO room (lodging_id, name, base_capacity, max_capacity, price_off_weekday, price_off_weekend, is_active, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
                """, Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, lodgingId); statement.setString(2, input.name().trim());
            statement.setInt(3, input.baseCapacity()); statement.setInt(4, input.maxCapacity());
            statement.setInt(5, input.priceWeekday()); statement.setInt(6, input.priceWeekend());
            return statement;
        }, key);
        long roomId = Objects.requireNonNull(key.getKey()).longValue();
        LocalDate today = LocalDate.now(KST);
        for (int offset = 0; offset < SALE_DAYS; offset++) {
            LocalDate day = today.plusDays(offset);
            jdbc.update("""
                INSERT INTO room_inventory (room_id, stay_date, total_quantity, held_quantity, reserved_quantity, is_on_sale, price, price_type)
                VALUES (?, ?, ?, 0, 0, TRUE, ?, 'OWNER_SET')
                """, roomId, day, input.quantity(), weekend(day) ? input.priceWeekend() : input.priceWeekday());
        }
        return roomId;
    }


    static boolean weekend(LocalDate day) {
        return day.getDayOfWeek() == DayOfWeek.FRIDAY || day.getDayOfWeek() == DayOfWeek.SATURDAY;
    }
}
