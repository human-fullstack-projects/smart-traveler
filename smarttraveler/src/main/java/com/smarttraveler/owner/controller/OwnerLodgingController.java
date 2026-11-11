//package com.smarttraveler.owner.controller;
//
//import com.smarttraveler.owner.dto.OwnerLodgingSummary;
//import com.smarttraveler.owner.service.OwnerLodgingService;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//import java.security.Principal;
//import java.util.List;
//
//@RestController
//@RequestMapping("/api/owner/lodgings")
//public class OwnerLodgingController {
//    private final OwnerLodgingService service;
//
//    public OwnerLodgingController(OwnerLodgingService service) {
//        this.service = service;
//    }
//
//    @GetMapping
//    public List<OwnerLodgingSummary> list(Principal principal) {
//        return service.findMyLodgings(principal.getName());
//   }
//}

package com.smarttraveler.owner.controller;

import com.smarttraveler.owner.dto.OwnerLodgingCreateRequest;
import com.smarttraveler.owner.dto.OwnerLodgingSummary;
import com.smarttraveler.owner.service.OwnerLodgingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.smarttraveler.owner.dto.OwnerLodgingDetail;
import org.springframework.web.bind.annotation.PathVariable;

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

    @GetMapping("/{lodgingId}")
    public OwnerLodgingDetail detail(
            Principal principal,
            @PathVariable("lodgingId") Long lodgingId
    ) {
        return service.findMyLodging(principal.getName(), lodgingId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OwnerLodgingSummary create(
            Principal principal,
            @Valid @RequestBody OwnerLodgingCreateRequest request
    ) {
        return service.create(principal.getName(), request);

    }


    @PutMapping("/{lodgingId}")
    public OwnerLodgingSummary update(
            Principal principal,
            @PathVariable("lodgingId") Long lodgingId,
            @Valid @RequestBody OwnerLodgingCreateRequest request
    ) {
        return service.update(principal.getName(), lodgingId, request);
    }
}