package com.smarttraveler.stay.controller;

import com.smarttraveler.stay.dto.StayResponse;
import com.smarttraveler.stay.service.StayService;
import com.smarttraveler.stay.service.StayInventoryService;
import com.smarttraveler.stay.service.StayInventoryService.RoomOption;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stays")
@RequiredArgsConstructor
public class StayController {
    private final StayService stayService;

    private final StayInventoryService inventory;

    @GetMapping
    public List<StayResponse> list(@RequestParam(defaultValue = "false") boolean bookable) {
        return bookable ? stayService.listWithInventory() : stayService.list();
    }

    @GetMapping("/{lodgingId}/rooms")
    public List<RoomOption> rooms(@PathVariable Long lodgingId, @RequestParam LocalDate checkIn,
                                 @RequestParam LocalDate checkOut, @RequestParam int guestCount) {
        if (guestCount < 1) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        stayService.get(lodgingId);
        return inventory.rooms(lodgingId, checkIn, checkOut, guestCount);
    }

    @GetMapping("/{lodgingId}")
    public StayResponse detail(@PathVariable Long lodgingId) {
        return stayService.get(lodgingId);
    }
}
