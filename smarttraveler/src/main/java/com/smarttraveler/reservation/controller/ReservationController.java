package com.smarttraveler.reservation.controller;

import com.smarttraveler.reservation.dto.ReservationResponse;
import com.smarttraveler.reservation.service.ReservationService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {
    private final ReservationService reservationService;

    @GetMapping("/{reservationId}")
    public ReservationResponse detail(@PathVariable Long reservationId) {
        return reservationService.get(reservationId);
    }

    @GetMapping("/member/{memberId}")
    public List<ReservationResponse> listByMember(@PathVariable Long memberId) {
        return reservationService.listByMember(memberId);
    }
}
