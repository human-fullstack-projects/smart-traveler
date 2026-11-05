package com.smarttraveler.admin.controller;

import com.smarttraveler.admin.service.HumanManagementService;
import java.security.Principal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminHumanManagementController {
    private final HumanManagementService service;
    public AdminHumanManagementController(HumanManagementService service) { this.service=service; }

    @GetMapping("/human-comments")
    public List<HumanManagementService.CommentCard> comments(@RequestParam(required=false) String region,
        @RequestParam(required=false) LocalDate from, @RequestParam(required=false) LocalDate to,
        @RequestParam(required=false) String query, @RequestParam(required=false) Long postId) {
        return service.comments(region,from,to,query,postId);
    }
    @GetMapping("/human-exposures")
    public List<HumanManagementService.Exposure> exposures(@RequestParam String scope,
        @RequestParam(required=false) String region) { return service.exposures(scope,region); }
    @PostMapping("/human-exposures")
    public Map<String,Object> exposure(@RequestBody ExposureChange body, Principal principal) {
        service.setExposure(body.postId(),body.scope(),body.region(),body.selected(),principal.getName());
        return Map.of("postId",body.postId(),"scope",body.scope(),"selected",body.selected());
    }
    @GetMapping("/human-popularity")
    public List<HumanManagementService.PopularCard> popularity(@RequestParam String month) {
        return service.popularity(month);
    }
    @GetMapping("/human-monthly-picks")
    public List<HumanManagementService.CourseCard> picks(@RequestParam String month) {
        return service.monthlyPicks(month);
    }
    @PostMapping("/human-monthly-picks")
    public Map<String,Object> pick(@RequestBody PickChange body, Principal principal) {
        service.setMonthlyPick(body.month(),body.postId(),body.selected(),principal.getName());
        return Map.of("postId",body.postId(),"month",body.month(),"selected",body.selected());
    }
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<Map<String,String>> invalid(RuntimeException error) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message",error.getMessage()));
    }
    public record ExposureChange(Long postId,String scope,String region,boolean selected) {}
    public record PickChange(String month,Long postId,boolean selected) {}
}
