package com.smarttraveler.chat;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/chat/guest-course")
public class GuestRecommendedCourseController {
    private final GuestRecommendedCourseService courses;
    private final ChatPersonalization personalization;
    public GuestRecommendedCourseController(GuestRecommendedCourseService courses, ChatPersonalization personalization) {
        this.courses = courses; this.personalization = personalization;
    }
    record Status(GuestRecommendedCourseService.Preview course) {}
    record Claimed(Long courseId) {}

    @GetMapping
    Status status(HttpSession session) { return new Status(courses.status(session, personalization.memberId())); }

    @PostMapping("/claim")
    Claimed claim(HttpSession session) {
        Long memberId = personalization.memberId();
        if (memberId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        return new Claimed(courses.claim(session, memberId));
    }
}
