package com.smarttraveler.place_photo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/** 관리자가 올린 관광지 사진 파일 (DB 저장 → 어느 PC 서버에서도 보임). place_image.image_url = /api/place-photos/{file_name} */
@Entity
@Table(name = "place_photo_file")
public class PlacePhotoFile {
    @Id
    @Column(name = "file_name", nullable = false, length = 64)
    private String fileName;

    @Column(name = "place_id", nullable = false)
    private Long placeId;

    @Column(name = "content_type", nullable = false, length = 32)
    private String contentType;

    @Lob
    @Column(name = "image_bytes", nullable = false, columnDefinition = "LONGBLOB")
    private byte[] imageBytes;

    @Column(name = "uploaded_by", length = 50)
    private String uploadedBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
