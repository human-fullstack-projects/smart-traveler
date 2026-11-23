package com.smarttraveler.owner_photo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * 업주가 올린 숙소·객실 사진 파일 (DB 저장 → 어느 PC 서버에서도 보임).
 * lodging_photo.image_url / room_photo.image_url = /api/owner-photos/{file_name}
 */
@Entity
@Table(name = "owner_photo_file")
public class OwnerPhotoFile {
    @Id
    @Column(name = "file_name", nullable = false, length = 64)
    private String fileName;

    /** LODGING 또는 ROOM */
    @Column(name = "target_type", nullable = false, length = 10)
    private String targetType;

    /** 숙소 번호(LODGING) 또는 객실 번호(ROOM) */
    @Column(name = "target_id", nullable = false)
    private Long targetId;

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
