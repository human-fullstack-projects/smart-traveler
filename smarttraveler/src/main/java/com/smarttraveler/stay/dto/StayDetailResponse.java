package com.smarttraveler.stay.dto;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

public record StayDetailResponse(
        Long lodgingId,
        Long regionId,
        String regionName,
        String sidoName,
        String name,
        String address,
        String lodgingType,
        BigDecimal latitude,
        BigDecimal longitude,
        LocalTime checkinTime,
        LocalTime checkoutTime,
        String thumbnailUrl,
        String photoUrl,
        String addressDetail,
        String zipcode,
        String phone,
        String homepageUrl,
        String description,
        String parkingInfo,
        String amenities,
        String refundPolicy,
        List<Photo> photos,
        List<Photo> roomPhotos
) {
    public record Photo(String imageUrl, String thumbnailUrl) {}
}
