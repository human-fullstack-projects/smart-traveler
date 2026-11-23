package com.smarttraveler.place_photo;

import java.util.Base64;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** 관리자 관광지 사진 등록 — 사진이 없는 관광지에만 대표 사진(sort_order 0) 1장. */
@Service
public class PlacePhotoService {
    public static final String URL_PREFIX = "/api/place-photos/";
    private static final int MAX_FILE_BYTES = 5 * 1024 * 1024;
    private static final int MAX_DATA_URL_LENGTH = 100 + 4 * ((MAX_FILE_BYTES + 2) / 3);
    private final JdbcTemplate jdbc;

    public PlacePhotoService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public record Upload(String name, String dataUrl) {}
    public record Stored(byte[] bytes, String contentType) {}

    @Transactional
    public String register(Long placeId, Upload photo, String adminLogin) {
        if (jdbc.queryForList("SELECT place_id FROM place WHERE place_id=? FOR UPDATE", Long.class, placeId).isEmpty())
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "관광지를 찾을 수 없습니다.");
        Integer has = jdbc.queryForObject("SELECT COUNT(*) FROM place_image WHERE place_id=?", Integer.class, placeId);
        if (has != null && has > 0) throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 사진이 있는 관광지예요.");
        byte[] bytes = decode(photo);
        String ext = extension(bytes);
        if (ext == null) throw bad();
        String name = UUID.randomUUID() + ext;
        jdbc.update("INSERT INTO place_photo_file(file_name,place_id,content_type,image_bytes,uploaded_by,created_at) VALUES (?,?,?,?,?,NOW())",
                name, placeId, contentType(ext), bytes, adminLogin);
        String url = URL_PREFIX + name;
        jdbc.update("INSERT INTO place_image(place_id,image_url,image_type,sort_order,source_provider,created_at) VALUES (?,?,'ADMIN_UPLOAD',0,'ADMIN',NOW())",
                placeId, url);
        return url;
    }

    public Stored get(String name) {
        if (!name.matches("[0-9a-f-]{36}\\.(jpg|png|webp|gif)")) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return jdbc.query("SELECT image_bytes, content_type FROM place_photo_file WHERE file_name=?",
                (rs, row) -> new Stored(rs.getBytes(1), rs.getString(2)), name)
                .stream().findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private static byte[] decode(Upload photo) {
        if (photo == null || photo.dataUrl() == null) throw bad();
        String data = photo.dataUrl();
        int comma = data.indexOf(',');
        if (comma < 0 || comma > 100 || !data.startsWith("data:")
                || !data.substring(0, comma).toLowerCase(Locale.ROOT).endsWith(";base64")) throw bad();
        if (data.length() > MAX_DATA_URL_LENGTH) throw tooLarge();
        byte[] bytes;
        try { bytes = Base64.getDecoder().decode(data.substring(comma + 1)); }
        catch (IllegalArgumentException e) { throw bad(); }
        if (bytes.length == 0) throw bad();
        if (bytes.length > MAX_FILE_BYTES) throw tooLarge();
        return bytes;
    }

    private static String contentType(String ext) {
        return switch (ext) { case ".png" -> "image/png"; case ".gif" -> "image/gif"; case ".webp" -> "image/webp"; default -> "image/jpeg"; };
    }

    private static String extension(byte[] b) {
        if (b.length >= 8 && (b[0] & 255) == 137 && b[1] == 80 && b[2] == 78 && b[3] == 71) return ".png";
        if (b.length >= 3 && (b[0] & 255) == 255 && (b[1] & 255) == 216 && (b[2] & 255) == 255) return ".jpg";
        if (b.length >= 6 && b[0] == 71 && b[1] == 73 && b[2] == 70 && b[3] == 56) return ".gif";
        if (b.length >= 12 && b[0] == 82 && b[1] == 73 && b[2] == 70 && b[3] == 70 && b[8] == 87 && b[9] == 69 && b[10] == 66 && b[11] == 80) return ".webp";
        return null;
    }

    private static ResponseStatusException bad() { return new ResponseStatusException(HttpStatus.BAD_REQUEST, "JPG·PNG·WebP·GIF 사진 파일을 골라 주세요."); }
    private static ResponseStatusException tooLarge() { return new ResponseStatusException(HttpStatus.BAD_REQUEST, "사진은 5MB까지 올릴 수 있어요."); }
}
