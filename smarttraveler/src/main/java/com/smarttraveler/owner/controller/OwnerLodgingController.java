package com.smarttraveler.owner.controller;

import com.smarttraveler.owner.dto.OwnerLodgingSummary;
import com.smarttraveler.owner.service.OwnerLodgingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/owner/lodgings")
public class OwnerLodgingController {
    private final OwnerLodgingService service;

    public OwnerLodgingController(OwnerLodgingService service) {
        this.service = service;
    }

    @GetMapping
    public List<OwnerLodgingSummary> list(Principal principal) {
        return service.findMyLodgings(principal.getName());
    }
}