package com.smarttraveler.recommendation;

import com.smarttraveler.recommendation.dto.RecommendationRequest;
import com.smarttraveler.recommendation.service.RecommendationService;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecommendationServiceTest {
    private final RecommendationService service = new RecommendationService();

    private RecommendationRequest request(List<RecommendationRequest.FixedEvent> fixedEvents) {
        return new RecommendationRequest(1L, LocalDate.of(2026, 11, 14), 2, 2, 0,
                "FRIENDS", "PUBLIC_TRANSPORT", List.of("바다"), fixedEvents, "COURSE_FIRST", null);
    }

    @Test
    void 실제_연결_전에는_추천_성공으로_응답하지_않는다() {
        var error = assertThrows(ResponseStatusException.class, () -> service.recommend(request(List.of())));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, error.getStatusCode());
    }

    @Test
    void 여행_기간_밖의_고정_일정은_거부한다() {
        var event = new RecommendationRequest.FixedEvent(LocalDate.of(2026, 11, 16), LocalTime.of(10, 0), "공연");
        var error = assertThrows(ResponseStatusException.class, () -> service.recommend(request(List.of(event))));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
    }

    @Test
    void 숙소_먼저_조건에는_선택한_숙소가_필요하다() {
        var request = new RecommendationRequest(1L, LocalDate.of(2026, 11, 14), 2, 2, 0,
                "FRIENDS", "PUBLIC_TRANSPORT", List.of(), List.of(), "STAY_FIRST", null);
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var violations = factory.getValidator().validate(request);
            assertTrue(violations.stream().anyMatch(violation ->
                    violation.getMessage().contains("숙소 먼저 방식")));
        }
    }
}
