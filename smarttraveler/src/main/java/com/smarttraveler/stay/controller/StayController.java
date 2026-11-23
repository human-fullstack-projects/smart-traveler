package com.smarttraveler.stay.controller;

import com.smarttraveler.stay.dto.StayDetailResponse;
import com.smarttraveler.stay.dto.StayResponse;
import com.smarttraveler.stay.dto.StaySearchDtos;
import com.smarttraveler.stay.service.StaySearchService;
import com.smarttraveler.stay.service.StayService;
import com.smarttraveler.stay.service.StayInventoryService;
import com.smarttraveler.stay.service.StayInventoryService.RoomOption;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stays")
@RequiredArgsConstructor
public class StayController {
    private final StayService stayService;

    private final StayInventoryService inventory;
    private final StaySearchService search;

    @GetMapping
    public List<StayResponse> list(@RequestParam(defaultValue = "false") boolean bookable) {
        return bookable ? stayService.listWithInventory() : stayService.list();
    }

    /** 여행자 숙소 찾기: 조건 검색 + 5/10곳씩 페이지 (기존 /api/stays 목록은 그대로). */
    @GetMapping("/search")
    public StaySearchDtos.Page search(@RequestParam(required = false) Long sido, @RequestParam(required = false) Long sigungu,
                                      @RequestParam(required = false) List<String> types, @RequestParam(required = false) List<String> amenities,
                                      @RequestParam(required = false) String keyword,
                                      @RequestParam LocalDate checkIn, @RequestParam LocalDate checkOut,
                                      @RequestParam(defaultValue = "2") int guests, @RequestParam(required = false) Integer maxPrice,
                                      @RequestParam(defaultValue = "false") boolean bookableOnly,
                                      @RequestParam(defaultValue = "distance") String sort,
                                      @RequestParam(required = false) Double lat, @RequestParam(required = false) Double lng,
                                      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        return search.search(query(sido, sigungu, types, amenities, keyword, checkIn, checkOut, guests, maxPrice, bookableOnly, sort, lat, lng), page, size);
    }

    /** 숙소 찾기 지도: 보이는 범위 안 숙소(많으면 묶음). 목록과 같은 조건을 쓴다. */
    @GetMapping("/map")
    public StaySearchDtos.MapResult map(@RequestParam(required = false) Long sido, @RequestParam(required = false) Long sigungu,
                                        @RequestParam(required = false) List<String> types, @RequestParam(required = false) List<String> amenities,
                                        @RequestParam(required = false) String keyword,
                                        @RequestParam LocalDate checkIn, @RequestParam LocalDate checkOut,
                                        @RequestParam(defaultValue = "2") int guests, @RequestParam(required = false) Integer maxPrice,
                                        @RequestParam(defaultValue = "false") boolean bookableOnly,
                                        @RequestParam double swLat, @RequestParam double swLng, @RequestParam double neLat, @RequestParam double neLng) {
        return search.map(query(sido, sigungu, types, amenities, keyword, checkIn, checkOut, guests, maxPrice, bookableOnly, "distance", null, null),
            swLat, swLng, neLat, neLng);
    }

    /** 숙소 찾기 지역 선택칸: 시·도 18개와 시·군·구 (숙소 수 포함). */
    @GetMapping("/regions")
    public List<StaySearchDtos.Region> regions() {
        return search.regions();
    }

    private static StaySearchService.Query query(Long sido, Long sigungu, List<String> types, List<String> amenities, String keyword,
                                                 LocalDate checkIn, LocalDate checkOut, int guests, Integer maxPrice, boolean bookableOnly,
                                                 String sort, Double lat, Double lng) {
        return new StaySearchService.Query(sido, sigungu, types == null ? List.of() : types, amenities == null ? List.of() : amenities,
            keyword, checkIn, checkOut, guests, maxPrice, bookableOnly, sort, lat, lng);
    }

    @GetMapping("/{lodgingId}/rooms")
    public List<RoomOption> rooms(@PathVariable Long lodgingId, @RequestParam LocalDate checkIn,
                                 @RequestParam LocalDate checkOut, @RequestParam int guestCount) {
        if (guestCount < 1) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        stayService.requireActive(lodgingId);
        return inventory.rooms(lodgingId, checkIn, checkOut, guestCount);
    }

    @GetMapping("/{lodgingId}")
    public StayDetailResponse detail(@PathVariable Long lodgingId) {
        return stayService.get(lodgingId);
    }
}
