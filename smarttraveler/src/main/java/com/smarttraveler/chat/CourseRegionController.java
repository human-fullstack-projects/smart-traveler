package com.smarttraveler.chat;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/chat/course-regions")
public class CourseRegionController {
    private final JdbcTemplate jdbc;
    public CourseRegionController(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public record Region(long regionId, String fullName) {}

    @GetMapping
    public List<Region> list() {
        return jdbc.query("""
            SELECT r.region_id, r.full_name FROM region r
            WHERE r.full_name IS NOT NULL AND r.is_supported=TRUE AND r.abolished_at IS NULL
              AND EXISTS (SELECT 1 FROM course c WHERE c.destination=r.full_name)
            ORDER BY r.full_name, r.region_id
            """, (rs, row) -> new Region(rs.getLong(1), rs.getString(2)));
    }

    Region find(long id) {
        return list().stream().filter(region -> region.regionId() == id).findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "추천 지역을 다시 선택해 주세요."));
    }
}
