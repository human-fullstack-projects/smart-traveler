package com.smarttraveler.recommendation.controller;

import com.smarttraveler.recommendation.dto.CourseDraft;
import com.smarttraveler.recommendation.dto.RecommendationRequest;
import com.smarttraveler.recommendation.service.RecommendationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 추천 요청의 입력 형식을 검증하고 추천 서비스에 전달한다. */
@RestController
@RequestMapping("/api/courses/recommendations")
public class RecommendationController {
    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @PostMapping
    public CourseDraft recommend(@Valid @RequestBody RecommendationRequest request) {
        return recommendationService.recommend(request);
    }
}
