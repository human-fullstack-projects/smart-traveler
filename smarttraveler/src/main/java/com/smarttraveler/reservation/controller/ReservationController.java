package com.smarttraveler.reservation.controller;

import com.smarttraveler.admin.service.ReservationRefundService;
import com.smarttraveler.reservation.dto.CreateReservationRequest;
import com.smarttraveler.reservation.dto.ReservationResponse;
import com.smarttraveler.reservation.service.ReservationService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {
    private final ReservationService reservationService;
    private final ReservationRefundService refunds;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservationResponse create(@Valid @RequestBody CreateReservationRequest request, Authentication auth) {
        return reservationService.create(request, auth);
    }

    @GetMapping("/{reservationId}")
    public ReservationResponse detail(@PathVariable Long reservationId, Authentication auth) {
        return reservationService.get(reservationId, auth);
    }

    @PostMapping("/{reservationId}/cancel")
    public ResponseEntity<Map<String, String>> cancel(@PathVariable Long reservationId, Authentication auth) {
        try {
            return ResponseEntity.ok(Map.of("status", refunds.cancelByMember(reservationId, auth.getName())));
        } catch (IllegalStateException error) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", error.getMessage()));
        }
    }

    @GetMapping("/member/{memberId}")
    public List<ReservationResponse> listByMember(@PathVariable Long memberId, Authentication auth) {
        return reservationService.listByMember(memberId, auth);
    }
}
