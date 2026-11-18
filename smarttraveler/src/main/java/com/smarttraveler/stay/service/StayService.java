package com.smarttraveler.stay.service;

import com.smarttraveler.domain.entity.Lodging;
import com.smarttraveler.stay.dto.StayResponse;
import com.smarttraveler.stay.repository.StayRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class StayService {
    private final StayRepository stayRepository;
    private final JdbcTemplate jdbc;

    public List<StayResponse> list() {
        return withPhotos(stayRepository.findByStatus("ACTIVE"));
    }

    public List<StayResponse> listWithInventory() {
        return withPhotos(stayRepository.findWithInventory());
    }

    public StayResponse get(Long lodgingId) {
        Lodging lodging = stayRepository.findById(lodgingId)
                .filter(stay -> "ACTIVE".equals(stay.getStatus()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return withPhotos(List.of(lodging)).get(0);
    }

    // 숙소별 대표 사진 1장(정렬 순서 → 사진 번호). 숙소 수가 많아도 500곳씩 나눠 한 번에 읽음.
    private List<StayResponse> withPhotos(List<Lodging> lodgings) {
        Map<Long, String> photos = new HashMap<>();
        List<Long> ids = lodgings.stream().map(Lodging::getLodgingId).toList();
        for (int from = 0; from < ids.size(); from += 500) {
            List<Long> chunk = new ArrayList<>(ids.subList(from, Math.min(from + 500, ids.size())));
            String marks = String.join(",", java.util.Collections.nCopies(chunk.size(), "?"));
            String sql = "SELECT p.lodging_id, COALESCE(NULLIF(p.thumbnail_url, ''), p.image_url) AS url"
                    + " FROM lodging_photo p WHERE p.lodging_id IN (" + marks + ")"
                    + " AND COALESCE(NULLIF(p.thumbnail_url, ''), NULLIF(p.image_url, '')) IS NOT NULL"
                    + " ORDER BY p.lodging_id, p.sort_order, p.photo_id";
            jdbc.query(sql, (org.springframework.jdbc.core.RowCallbackHandler) rs ->
                    photos.putIfAbsent(rs.getLong("lodging_id"), rs.getString("url")), chunk.toArray());
        }
        return lodgings.stream().map(l -> toResponse(l, photos.get(l.getLodgingId()))).toList();
    }

    private StayResponse toResponse(Lodging lodging, String thumbnailUrl) {
        return new StayResponse(
                lodging.getLodgingId(),
                lodging.getName(),
                lodging.getAddress(),
                lodging.getLodgingType(),
                lodging.getLatitude(),
                lodging.getLongitude(),
                lodging.getCheckinTime(),
                lodging.getCheckoutTime(),
                thumbnailUrl
        );
    }
}
