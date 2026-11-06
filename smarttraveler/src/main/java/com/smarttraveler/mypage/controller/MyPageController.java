package com.smarttraveler.mypage.controller;

import com.smarttraveler.mypage.dto.MyPageDtos.*;
import com.smarttraveler.mypage.service.MyPageService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/mypage")
public class MyPageController {
    private final MyPageService service;
    public MyPageController(MyPageService service) { this.service = service; }

    @GetMapping
    public Snapshot snapshot(Authentication auth, @RequestHeader(value = "X-MyPage-Member", required = false) Long expected) {
        return service.snapshot(auth, expected);
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
