package com.smarttraveler.admin.controller;

import com.smarttraveler.admin.service.HumanManagementService;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/human-courses")
public class PublicHumanReadController {
    private final HumanManagementService service;
    public PublicHumanReadController(HumanManagementService service) { this.service=service; }

    @GetMapping("/exposed")
    public List<HumanManagementService.CourseCard> exposed(@RequestParam String scope,
        @RequestParam(required=false) String region) { return service.publicExposures(scope,region); }
    @GetMapping("/monthly-picks")
    public List<HumanManagementService.CourseCard> monthlyPicks(@RequestParam String month) {
        return service.monthlyPicks(month);
    }
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String,String>> invalid(IllegalArgumentException error) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message",error.getMessage()));
    }
}
