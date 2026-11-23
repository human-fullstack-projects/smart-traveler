package com.smarttraveler.place_photo;

import java.security.Principal;
import java.time.Duration;
import java.util.Map;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PlacePhotoController {
    private final PlacePhotoService photos;

    public PlacePhotoController(PlacePhotoService photos) { this.photos = photos; }

    /** 관리자: 사진 없는 관광지에 사진 등록 (/api/admin/** = ADMIN만). */
    @PostMapping("/api/admin/places/{placeId}/photo")
    public Map<String, Object> upload(@PathVariable Long placeId, @RequestBody PlacePhotoService.Upload body, Principal principal) {
        return Map.of("placeId", placeId, "imageUrl", photos.register(placeId, body, principal.getName()));
    }

    /** 누구나: 올린 관광지 사진 보기. */
    @GetMapping("/api/place-photos/{name:.+}")
    public ResponseEntity<byte[]> photo(@PathVariable String name) {
        PlacePhotoService.Stored s = photos.get(name);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(s.contentType()))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(7))).body(s.bytes());
    }
}
