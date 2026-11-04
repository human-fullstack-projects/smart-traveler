package com.smarttraveler.recommendation.service;

import com.smarttraveler.recommendation.dto.CourseDraft;
import com.smarttraveler.recommendation.dto.RecommendationRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** 실제 장소 검색과 AI가 준비되기 전에는 추천이 성공한 것처럼 반환하지 않는다. */
@Service
public class RecommendationService {
    public CourseDraft recommend(RecommendationRequest request) {
        // 고정 일정은 여행 기간 안에 있어야 한다.
        var endDate = request.startDate().plusDays(request.durationDays() - 1L);
        if (request.fixedEvents().stream().anyMatch(event ->
                event.date().isBefore(request.startDate()) || event.date().isAfter(endDate))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "고정 일정은 여행 기간 안에 있어야 합니다.");
        }

        // 실제 검색·생성 구현체를 연결할 때 후보 검증과 결과 검증을 이곳에 추가한다.
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "일정 추천 기능은 연결 준비 중입니다.");
    }
}
