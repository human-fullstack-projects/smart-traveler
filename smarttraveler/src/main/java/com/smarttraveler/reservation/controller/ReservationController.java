package com.smarttraveler.reservation.controller;

import com.smarttraveler.reservation.dto.CreateReservationRequest;
import com.smarttraveler.reservation.dto.ReservationResponse;
import com.smarttraveler.reservation.service.ReservationService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {
    private final ReservationService reservationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservationResponse create(@Valid @RequestBody CreateReservationRequest request, Authentication auth) {
        return reservationService.create(request, auth);
    }

    @GetMapping("/{reservationId}")
    public ReservationResponse detail(@PathVariable Long reservationId, Authentication auth) {
        return reservationService.get(reservationId, auth);
    }

    @GetMapping("/member/{memberId}")
    public List<ReservationResponse> listByMember(@PathVariable Long memberId, Authentication auth) {
        return reservationService.listByMember(memberId, auth);
    }
}
