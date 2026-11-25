package com.smarttraveler.stay.service;

import com.smarttraveler.domain.entity.Lodging;
import com.smarttraveler.domain.entity.Region;
import com.smarttraveler.stay.dto.StayDetailResponse;
import com.smarttraveler.stay.dto.StayDetailResponse.Photo;
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

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public StayDetailResponse get(Long lodgingId) {
        Lodging lodging = findActive(lodgingId);
        List<Photo> photos = jdbc.query("""
            SELECT COALESCE(NULLIF(thumbnail_url, ''), image_url) AS thumbnail_url,
                   COALESCE(NULLIF(image_url, ''), thumbnail_url) AS photo_url
            FROM lodging_photo
            WHERE lodging_id=? AND COALESCE(NULLIF(thumbnail_url, ''), NULLIF(image_url, '')) IS NOT NULL
            ORDER BY sort_order, photo_id
            """, (rs, row) -> new Photo(rs.getString("photo_url"), rs.getString("thumbnail_url")), lodgingId);
        Photo cover = photos.isEmpty() ? new Photo(null, null) : photos.getFirst();
        List<Photo> roomPhotos = jdbc.query("""
            SELECT NULLIF(TRIM(p.image_url), '') AS photo_url, NULLIF(TRIM(p.thumbnail_url), '') AS thumbnail_url
            FROM room_photo p JOIN room r ON r.room_id=p.room_id
            WHERE r.lodging_id=? AND r.is_active=TRUE
              AND COALESCE(NULLIF(TRIM(p.image_url), ''), NULLIF(TRIM(p.thumbnail_url), '')) IS NOT NULL
            ORDER BY r.room_id, p.sort_order, p.photo_id
            """, (rs, row) -> new Photo(rs.getString("photo_url"), rs.getString("thumbnail_url")), lodgingId);

        Region region = lodging.getRegion();
        return new StayDetailResponse(
            lodging.getLodgingId(), lodging.getRegionId(),
            region == null ? null : region.getFullName(), sidoName(region),
            lodging.getName(), lodging.getAddress(), lodging.getLodgingType(),
            lodging.getLatitude(), lodging.getLongitude(), lodging.getCheckinTime(), lodging.getCheckoutTime(),
            cover.thumbnailUrl(), cover.imageUrl(), lodging.getAddressDetail(), lodging.getZipcode(),
            lodging.getPhone(), lodging.getHomepageUrl(), lodging.getDescription(), lodging.getParkingInfo(),
            lodging.getAmenities(), lodging.getRefundPolicy(), photos, roomPhotos
        );
    }

    private static String sidoName(Region region) {
        while (region != null && !"SIDO".equals(region.getRegionLevel())) {
            region = region.getParentRegion();
        }
        return region == null ? null : region.getRegionName();
    }

    public Lodging requireActive(Long lodgingId) {
        return findActive(lodgingId);
    }

    private Lodging findActive(Long lodgingId) {
        return stayRepository.findById(lodgingId)
                .filter(stay -> "ACTIVE".equals(stay.getStatus()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
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
