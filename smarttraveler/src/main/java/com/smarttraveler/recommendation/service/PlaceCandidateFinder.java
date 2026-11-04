package com.smarttraveler.recommendation.service;

import com.smarttraveler.recommendation.dto.RecommendationRequest;

import java.util.List;

/** 확인된 조건으로 실제 장소를 검색한다. DB와 RAG 검색은 이후 연결한다. */
public interface PlaceCandidateFinder {
    List<PlaceCandidate> findCandidates(RecommendationRequest request);

    record PlaceCandidate(Long placeId, String name, String description) {}
}
