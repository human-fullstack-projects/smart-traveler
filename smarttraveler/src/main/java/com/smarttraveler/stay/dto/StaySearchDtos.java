package com.smarttraveler.stay.dto;

import java.math.BigDecimal;
import java.util.List;

/** 여행자 숙소 찾기(조건 검색 · 페이지 · 지도) 응답. 기존 /api/stays 응답(StayResponse)은 그대로 둔다. */
public final class StaySearchDtos {
    private StaySearchDtos() {}

    /** 목록 카드 1장. saleable=false면 선택 날짜·인원에 판매 중인 객실이 없음. */
    public record Item(Long lodgingId, String name, String address, String lodgingType, String regionName,
                       BigDecimal latitude, BigDecimal longitude, String thumbnailUrl, List<String> amenities,
                       boolean saleable, Integer minPrice, Integer remaining) {}

    /** bounds = 조건에 맞는 숙소 전체가 들어가는 지도 범위 [남서 위도, 남서 경도, 북동 위도, 북동 경도] (없으면 null). */
    public record Page(List<Item> items, long total, int page, int size, int totalPages, double[] bounds) {}

    /** 지도 점: count==1이면 숙소 1곳(lodgingId·name 있음), 그보다 크면 묶음. */
    public record MapPoint(Long lodgingId, String name, double latitude, double longitude, int count) {}

    public record MapResult(List<MapPoint> points, long total, boolean clustered) {}

    public record Region(Long regionId, String name, long count, List<Region> children) {}
}
