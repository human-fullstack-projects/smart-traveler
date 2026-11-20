package com.smarttraveler.owner.controller;

import com.smarttraveler.owner.dto.OwnerRoomDtos.InventoryDay;
import com.smarttraveler.owner.dto.OwnerRoomDtos.InventoryUpdate;
import com.smarttraveler.owner.dto.OwnerRoomDtos.OwnerReservation;
import com.smarttraveler.owner.dto.OwnerRoomDtos.OwnerRoom;
import com.smarttraveler.owner.dto.OwnerRoomDtos.RoomInput;
import com.smarttraveler.owner.service.OwnerRoomService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** 업주 객실 · 날짜별 판매 수량 · 내 숙소 예약. */
@RestController
@RequestMapping("/api/owner")
public class OwnerRoomController {
    private final OwnerRoomService service;

    public OwnerRoomController(OwnerRoomService service) {
        this.service = service;
    }

    @GetMapping("/rooms")
    public List<OwnerRoom> rooms(Principal principal) {
        return service.rooms(principal.getName());
    }

    @PostMapping("/lodgings/{lodgingId}/rooms")
    @ResponseStatus(HttpStatus.CREATED)
    public OwnerRoom addRoom(Principal principal, @PathVariable("lodgingId") long lodgingId, @Valid @RequestBody RoomInput input) {
        return service.addRoom(principal.getName(), lodgingId, input);
    }

    @GetMapping("/rooms/{roomId}/inventory")
    public List<InventoryDay> inventory(Principal principal, @PathVariable("roomId") long roomId,
                                        @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                        @RequestParam(name = "days", defaultValue = "14") int days) {
        return service.inventory(principal.getName(), roomId, from, days);
    }

    @PutMapping("/rooms/{roomId}/inventory")
    public List<InventoryDay> setQuantity(Principal principal, @PathVariable("roomId") long roomId, @Valid @RequestBody InventoryUpdate update) {
        return service.setQuantity(principal.getName(), roomId, update);
    }

    @GetMapping("/reservations")
    public List<OwnerReservation> reservations(Principal principal) {
        return service.reservations(principal.getName());
    }
}
