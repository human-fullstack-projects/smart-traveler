package com.smarttraveler.human_course;

import com.smarttraveler.human_course.HumanCourseDtos.*;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/human-courses")
public class HumanCourseController {
    private final HumanCourseService service;
    private final HumanCoursePhotoStore photos;

    public HumanCourseController(HumanCourseService service, HumanCoursePhotoStore photos) {
        this.service = service; this.photos = photos;
    }

    @GetMapping
    public List<Post> list(Authentication auth) { return service.list(auth); }

        @GetMapping("/home-reviews")
    public List<Post> homeReviews(Authentication auth) {
        return service.homeReviews(auth);
    }

    @GetMapping("/eligibility")
    public List<Reservation> eligibility(Authentication auth) { return service.reservations(auth); }

    @GetMapping("/photos/{name:.+}")
    public ResponseEntity<Resource> photo(@PathVariable String name, Authentication auth) {
        service.requirePhotoReadable(name, auth);
        MediaType type = name.endsWith(".png") ? MediaType.IMAGE_PNG : name.endsWith(".gif") ? MediaType.IMAGE_GIF
            : name.endsWith(".webp") ? MediaType.parseMediaType("image/webp") : MediaType.IMAGE_JPEG;
        return ResponseEntity.ok().contentType(type).body(photos.get(name));
    }

    @GetMapping("/{id}")
    public Post detail(@PathVariable long id, Authentication auth) { return service.detail(id, auth); }

    @PostMapping("/reservations/{reservationId}")
    @ResponseStatus(HttpStatus.CREATED)
    public IdResult create(@PathVariable long reservationId, Authentication auth, @RequestBody PostInput input) {
        return service.create(auth, reservationId, input);
    }

    @PutMapping("/{id}")
    public IdResult edit(@PathVariable long id, Authentication auth, @RequestBody PostInput input) {
        return service.edit(auth, id, input);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id, Authentication auth) { service.delete(auth, id); }

    @PostMapping("/{id}/like")
    public Post like(@PathVariable long id, Authentication auth) { return service.like(auth, id); }

    @PostMapping("/{id}/comments")
    public Post comment(@PathVariable long id, Authentication auth, @RequestBody CommentInput input) {
        return service.comment(auth, id, input);
    }

    @DeleteMapping("/{id}/comments/{commentId}")
    public Post deleteComment(@PathVariable long id, @PathVariable long commentId, Authentication auth) {
        return service.deleteComment(auth, id, commentId);
    }

    @PostMapping("/{id}/reports")
    public Post report(@PathVariable long id, Authentication auth, @RequestBody ReportInput input) {
        return service.report(auth, id, input);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> error(ResponseStatusException exception) {
        return ResponseEntity.status(exception.getStatusCode())
            .body(Map.of("message", exception.getReason() == null ? "요청을 처리하지 못했습니다." : exception.getReason()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> conflict() {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(Map.of("message", "이미 작성하거나 처리된 항목입니다. 새로고침 후 다시 확인해주세요."));
    }
}
