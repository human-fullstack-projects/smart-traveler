package com.smarttraveler.recommendation.service;

import com.smarttraveler.recommendation.dto.CourseDraft;
import com.smarttraveler.recommendation.dto.RecommendationRequest;

import java.util.List;

/** 검색된 장소를 이용해 일정 초안을 생성한다. OpenAI 연결은 이후 구현한다. */
public interface AiCourseGenerator {
    CourseDraft generate(RecommendationRequest request, List<PlaceCandidateFinder.PlaceCandidate> candidates);
}
