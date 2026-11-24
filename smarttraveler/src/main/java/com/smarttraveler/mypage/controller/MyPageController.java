package com.smarttraveler.mypage.controller;

import com.smarttraveler.mypage.dto.MyPageDtos.*;
import com.smarttraveler.mypage.service.MyPageService;
import com.smarttraveler.tour.service.TourPhotoService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/mypage")
public class MyPageController {
    private final MyPageService service;
    private final TourPhotoService photos;

    public MyPageController(MyPageService service, TourPhotoService photos) {
        this.service = service;
        this.photos = photos;
    }

    @GetMapping
    public Snapshot snapshot(Authentication auth, @RequestHeader(value = "X-MyPage-Member", required = false) Long expected) {
        return service.snapshot(auth, expected);
    }
    @GetMapping("/places/detail")
    public PlaceDetails placeDetails(Authentication auth, @RequestParam(required=false) Long placeId,
            @RequestParam(defaultValue="") String name, @RequestParam(defaultValue="") String region) {
        return service.placeDetails(auth,placeId,name,region);
    }

    @GetMapping("/places/tour-photos")
    public List<String> placePhotos(Authentication auth, @RequestParam(required = false) Long placeId,
            @RequestParam(defaultValue = "") String name, @RequestParam(defaultValue = "") String address,
            @RequestParam(defaultValue = "") String region) {
        return photos.photos(service.placePhotoTarget(auth, placeId, name, address, region));
    }

    @GetMapping("/reservations/{id}/tour-photos")
    public List<String> lodgingPhotos(Authentication auth, @PathVariable String id) {
        var target = service.lodgingPhotoTarget(auth, id);
        return target == null ? List.of() : photos.photos(target);
    }
    @GetMapping("/courses/{id}")
    public CourseView course(Authentication auth, @RequestHeader(value = "X-MyPage-Member", required = false) Long expected, @PathVariable String id) {
        return service.course(auth, expected, id);
    }
    @GetMapping("/reservations/{id}")
    public BookingView booking(Authentication auth, @RequestHeader(value = "X-MyPage-Member", required = false) Long expected, @PathVariable String id) {
        return service.booking(auth, expected, id);
    }
    @GetMapping("/posts/{id}")
    public PostView post(Authentication auth, @RequestHeader(value = "X-MyPage-Member", required = false) Long expected, @PathVariable String id) {
        return service.post(auth, expected, id);
    }
    @PostMapping("/courses/import")
    public ImportResult importCourses(Authentication auth, @Valid @RequestBody ImportRequest request) {
        return service.importCourses(auth, request);
    }
}
