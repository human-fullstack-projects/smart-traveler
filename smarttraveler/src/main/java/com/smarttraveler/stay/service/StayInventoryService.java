package com.smarttraveler.stay.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class StayInventoryService {
    private final JdbcTemplate jdbc;

    public record RoomOption(long roomId, String name, int maxCapacity, int remaining,
                             int totalPrice, List<RoomNight> nights) {}
    public record RoomNight(long inventoryId, LocalDate date, int price, int remaining, boolean onSale) {}
    private record RoomInfo(long id, String name, int maxCapacity) {}

    public List<RoomOption> rooms(long lodgingId, LocalDate checkIn, LocalDate checkOut, int guests) {
        validateDates(checkIn, checkOut);
        var rooms = jdbc.query("""
            SELECT room_id, name, max_capacity FROM room
            WHERE lodging_id=? AND is_active=TRUE AND max_capacity>=?
            ORDER BY room_id
            """, (rs, row) -> new RoomInfo(rs.getLong("room_id"), rs.getString("name"),
                rs.getInt("max_capacity")), lodgingId, guests);
        List<RoomOption> options = new ArrayList<>();
        for (RoomInfo room : rooms) {
            var nights = nights(room.id(), checkIn, checkOut, false);
            if (available(nights, checkIn, checkOut)) {
                int remaining = nights.stream().mapToInt(RoomNight::remaining).min().orElse(0);
                options.add(new RoomOption(room.id(), room.name(), room.maxCapacity(), remaining,
                        total(nights), nights));
            }
        }
        return options;
    }

    public List<RoomNight> nights(long roomId, LocalDate checkIn, LocalDate checkOut, boolean lock) {
        String sql = """
            SELECT inventory_id, stay_date, price, is_on_sale,
                   total_quantity - held_quantity - reserved_quantity AS remaining
            FROM room_inventory
            WHERE room_id=? AND stay_date>=? AND stay_date<?
            ORDER BY stay_date
            """;
        return jdbc.query(sql + (lock ? " FOR UPDATE" : ""),
                (rs, row) -> new RoomNight(rs.getLong("inventory_id"),
                        rs.getObject("stay_date", LocalDate.class), rs.getInt("price"),
                        rs.getInt("remaining"), rs.getBoolean("is_on_sale")),
                roomId, checkIn, checkOut);
    }

    public void validateDates(LocalDate checkIn, LocalDate checkOut) {
        if (checkIn.isBefore(LocalDate.now(ZoneId.of("Asia/Seoul")))
                || !checkOut.isAfter(checkIn) || checkOut.isAfter(checkIn.plusDays(30))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "숙박 날짜를 다시 확인해 주세요.");
        }
    }

    public boolean available(List<RoomNight> nights, LocalDate checkIn, LocalDate checkOut) {
        return nights.size() == ChronoUnit.DAYS.between(checkIn, checkOut)
                && nights.stream().allMatch(night -> night.onSale() && night.remaining() > 0 && night.price() > 0);
    }

    public int total(List<RoomNight> nights) {
        return nights.stream().mapToInt(RoomNight::price).reduce(0, Math::addExact);
    }
}
