package com.smarttraveler.admin;

import com.smarttraveler.admin.repository.RecommendationCriteriaRepository;
import com.smarttraveler.admin.service.RecommendationCriteriaDefaults;
import com.smarttraveler.domain.entity.RecommendationCriteria;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RecommendationCriteriaDefaultsTest {
    @Test void existingNamesAreNeverOverwrittenAndMissingNamesAreInsertedOnce() throws Exception {
        RecommendationCriteriaRepository repository = mock(RecommendationCriteriaRepository.class);
        when(repository.existsByName("추천 제외 장소")).thenReturn(true);
        RecommendationCriteriaDefaults runner = new RecommendationCriteriaDefaults(repository);
        runner.run(new DefaultApplicationArguments());
        verify(repository, times(4)).save(any(RecommendationCriteria.class));
        verify(repository, never()).findById(any());
        for (String name : RecommendationCriteriaDefaults.DEFAULTS.keySet()) {
            when(repository.existsByName(name)).thenReturn(true);
        }
        runner.run(new DefaultApplicationArguments());
        verify(repository, times(4)).save(any(RecommendationCriteria.class));
    }
}
