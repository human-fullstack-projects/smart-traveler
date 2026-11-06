package com.smarttraveler.admin.service;

import com.smarttraveler.admin.repository.RecommendationCriteriaRepository;
import com.smarttraveler.domain.entity.RecommendationCriteria;
import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class RecommendationCriteriaDefaults implements ApplicationRunner {
    // Plain text until the course generation owner agrees on a machine-readable format.
    public static final Map<String, String> DEFAULTS = Map.of(
        "관광지 데이터 범위", "경주시(47130)",
        "추천 제외 장소", "없음",
        "답변 범위 제한", "여행·관광·숙소 관련 질문만 답변",
        "교통수단별 최대 이동 시간", "도보 20분, 대중교통 40분, 자동차 30분",
        "숙소 추천 범위 확대 단계", "1km → 3km → 5km"
    );

    private final RecommendationCriteriaRepository repository;

    public RecommendationCriteriaDefaults(RecommendationCriteriaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        DEFAULTS.forEach((name, value) -> {
            if (repository.existsByName(name)) return;
            RecommendationCriteria criterion = new RecommendationCriteria();
            criterion.setName(name);
            criterion.setValue(value);
            criterion.setUpdatedAt(LocalDateTime.now());
            repository.save(criterion);
        });
    }
}
