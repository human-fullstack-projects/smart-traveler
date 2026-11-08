package com.smarttraveler.stay.service;

import com.smarttraveler.domain.entity.Lodging;
import com.smarttraveler.stay.dto.StayResponse;
import com.smarttraveler.stay.repository.StayRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StayService {
    private final StayRepository stayRepository;

    public List<StayResponse> list() {
        return stayRepository.findByStatus("ACTIVE").stream()
                .map(this::toResponse)
                .toList();
    }

    public StayResponse get(Long lodgingId) {
        Lodging lodging = stayRepository.findById(lodgingId).get();
        return toResponse(lodging);
    }

    private StayResponse toResponse(Lodging lodging) {
        return new StayResponse(
                lodging.getLodgingId(),
                lodging.getName(),
                lodging.getAddress(),
                lodging.getLodgingType(),
                lodging.getLatitude(),
                lodging.getLongitude(),
                lodging.getCheckinTime(),
                lodging.getCheckoutTime()
        );
    }
}
