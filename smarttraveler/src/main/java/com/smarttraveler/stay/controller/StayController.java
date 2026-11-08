package com.smarttraveler.stay.controller;

import com.smarttraveler.stay.dto.StayResponse;
import com.smarttraveler.stay.service.StayService;
import java.util.List;
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

    @GetMapping
    public List<StayResponse> list() {
        return stayService.list();
    }

    @GetMapping("/{lodgingId}")
    public StayResponse detail(@PathVariable Long lodgingId) {
        return stayService.get(lodgingId);
    }
}
