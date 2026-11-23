package com.smarttraveler.owner_photo;

import com.smarttraveler.owner_photo.OwnerPhotoService.Photo;
import com.smarttraveler.owner_photo.OwnerPhotoService.Upload;
import java.security.Principal;
import java.time.Duration;
import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** 업주: 내 숙소·객실 사진 보기·올리기·지우기 / 누구나: 올린 사진 파일 보기. */
@RestController
public class OwnerPhotoController {
    private final OwnerPhotoService photos;

    public OwnerPhotoController(OwnerPhotoService photos) { this.photos = photos; }

    @GetMapping("/api/owner/lodgings/{lodgingId}/photos")
    public List<Photo> lodgingPhotos(Principal principal, @PathVariable("lodgingId") long lodgingId) {
        return photos.lodgingPhotos(principal.getName(), lodgingId);
    }

    @PostMapping("/api/owner/lodgings/{lodgingId}/photos")
    public List<Photo> addLodgingPhoto(Principal principal, @PathVariable("lodgingId") long lodgingId, @RequestBody Upload body) {
        return photos.addLodgingPhoto(principal.getName(), lodgingId, body);
    }

    @DeleteMapping("/api/owner/lodgings/{lodgingId}/photos/{photoId}")
    public List<Photo> deleteLodgingPhoto(Principal principal, @PathVariable("lodgingId") long lodgingId, @PathVariable("photoId") long photoId) {
        return photos.deleteLodgingPhoto(principal.getName(), lodgingId, photoId);
    }

    @GetMapping("/api/owner/rooms/{roomId}/photos")
    public List<Photo> roomPhotos(Principal principal, @PathVariable("roomId") long roomId) {
        return photos.roomPhotos(principal.getName(), roomId);
    }

    @PostMapping("/api/owner/rooms/{roomId}/photos")
    public List<Photo> addRoomPhoto(Principal principal, @PathVariable("roomId") long roomId, @RequestBody Upload body) {
        return photos.addRoomPhoto(principal.getName(), roomId, body);
    }

    @DeleteMapping("/api/owner/rooms/{roomId}/photos/{photoId}")
    public List<Photo> deleteRoomPhoto(Principal principal, @PathVariable("roomId") long roomId, @PathVariable("photoId") long photoId) {
        return photos.deleteRoomPhoto(principal.getName(), roomId, photoId);
    }

    @GetMapping("/api/owner-photos/{name:.+}")
    public ResponseEntity<byte[]> photo(@PathVariable("name") String name) {
        OwnerPhotoService.Stored s = photos.get(name);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(s.contentType()))
            .cacheControl(CacheControl.maxAge(Duration.ofDays(7))).body(s.bytes());
    }
}
