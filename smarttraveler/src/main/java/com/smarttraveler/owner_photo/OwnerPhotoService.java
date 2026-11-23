package com.smarttraveler.owner_photo;

import com.smarttraveler.owner.service.OwnerLodgingService;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * 업주 숙소·객실 사진 등록 (업주 피드백 「숙소·객실 사진 등록」).
 * 파일은 owner_photo_file 에 저장하고, 여행자 화면이 이미 읽는 lodging_photo / room_photo 에 주소 한 줄을 넣는다.
 * 새로 올린 사진이 맨 앞(대표 사진)에 온다. 업주가 올린 사진만 지울 수 있다 (관광공사 등 기존 사진은 그대로).
 */
@Service
public class OwnerPhotoService {
    public static final String URL_PREFIX = "/api/owner-photos/";
    public static final String SOURCE = "OWNER_UPLOAD";
    static final int MAX_PHOTOS = 10;
    private static final int MAX_FILE_BYTES = 5 * 1024 * 1024;
    private static final int MAX_DATA_URL_LENGTH = 100 + 4 * ((MAX_FILE_BYTES + 2) / 3);
    private final JdbcTemplate jdbc;
    private final OwnerLodgingService lodgings;

    public OwnerPhotoService(JdbcTemplate jdbc, OwnerLodgingService lodgings) {
        this.jdbc = jdbc;
        this.lodgings = lodgings;
    }

    public record Upload(String name, String dataUrl) {}
    public record Photo(long photoId, String imageUrl, String thumbnailUrl, boolean uploaded) {}
    public record Stored(byte[] bytes, String contentType) {}

    /** 숙소면 lodging_photo, 객실이면 room_photo. */
    private enum Target {
        LODGING("lodging_photo", "lodging_id", "SELECT COUNT(*) FROM lodging WHERE lodging_id = ? AND owner_id = ? FOR UPDATE", "숙소를 찾을 수 없습니다."),
        ROOM("room_photo", "room_id",
            "SELECT COUNT(*) FROM room r JOIN lodging l ON l.lodging_id = r.lodging_id WHERE r.room_id = ? AND l.owner_id = ? FOR UPDATE",
            "객실을 찾을 수 없습니다.");
        final String table, idColumn, ownerCheck, notFound;
        Target(String table, String idColumn, String ownerCheck, String notFound) {
            this.table = table; this.idColumn = idColumn; this.ownerCheck = ownerCheck; this.notFound = notFound;
        }
    }

    @Transactional(readOnly = true)
    public List<Photo> lodgingPhotos(String loginId, long lodgingId) { return list(Target.LODGING, loginId, lodgingId); }

    @Transactional(readOnly = true)
    public List<Photo> roomPhotos(String loginId, long roomId) { return list(Target.ROOM, loginId, roomId); }

    @Transactional
    public List<Photo> addLodgingPhoto(String loginId, long lodgingId, Upload upload) { return add(Target.LODGING, loginId, lodgingId, upload); }

    @Transactional
    public List<Photo> addRoomPhoto(String loginId, long roomId, Upload upload) { return add(Target.ROOM, loginId, roomId, upload); }

    @Transactional
    public List<Photo> deleteLodgingPhoto(String loginId, long lodgingId, long photoId) { return delete(Target.LODGING, loginId, lodgingId, photoId); }

    @Transactional
    public List<Photo> deleteRoomPhoto(String loginId, long roomId, long photoId) { return delete(Target.ROOM, loginId, roomId, photoId); }

    public Stored get(String name) {
        if (!name.matches("[0-9a-f-]{36}\\.(jpg|png|webp|gif)")) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return jdbc.query("SELECT image_bytes, content_type FROM owner_photo_file WHERE file_name=?",
                (rs, row) -> new Stored(rs.getBytes(1), rs.getString(2)), name)
            .stream().findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private List<Photo> list(Target target, String loginId, long id) {
        requireMine(target, loginId, id, false);
        return read(target, id);
    }

    private List<Photo> read(Target target, long id) {
        return jdbc.query("SELECT photo_id, image_url, thumbnail_url, source_provider FROM " + target.table
                + " WHERE " + target.idColumn + " = ? ORDER BY sort_order, photo_id",
            (rs, row) -> new Photo(rs.getLong("photo_id"), rs.getString("image_url"), rs.getString("thumbnail_url"),
                SOURCE.equals(rs.getString("source_provider"))), id);
    }

    private List<Photo> add(Target target, String loginId, long id, Upload upload) {
        requireMine(target, loginId, id, true);
        Integer uploaded = jdbc.queryForObject("SELECT COUNT(*) FROM " + target.table + " WHERE " + target.idColumn
            + " = ? AND source_provider = ?", Integer.class, id, SOURCE);
        if (uploaded != null && uploaded >= MAX_PHOTOS)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "사진은 " + MAX_PHOTOS + "장까지 올릴 수 있어요. 안 쓰는 사진을 지운 뒤 올려 주세요.");
        byte[] bytes = decode(upload);
        String ext = extension(bytes);
        if (ext == null) throw bad();
        String name = UUID.randomUUID() + ext;
        jdbc.update("INSERT INTO owner_photo_file(file_name,target_type,target_id,content_type,image_bytes,uploaded_by,created_at) VALUES (?,?,?,?,?,?,NOW())",
            name, target.name(), id, contentType(ext), bytes, loginId);
        Integer first = jdbc.queryForObject("SELECT COALESCE(MIN(sort_order), 1) FROM " + target.table + " WHERE " + target.idColumn + " = ?",
            Integer.class, id);
        int sort = Math.min(first == null ? 1 : first, 1) - 1;
        jdbc.update("INSERT INTO " + target.table + "(" + target.idColumn + ",image_url,source_provider,sort_order,created_at) VALUES (?,?,?,?,NOW())",
            id, URL_PREFIX + name, SOURCE, sort);
        return read(target, id);
    }

    private List<Photo> delete(Target target, String loginId, long id, long photoId) {
        requireMine(target, loginId, id, true);
        List<String> urls = jdbc.queryForList("SELECT image_url FROM " + target.table + " WHERE photo_id = ? AND " + target.idColumn
            + " = ? AND source_provider = ?", String.class, photoId, id, SOURCE);
        if (urls.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "직접 올린 사진만 지울 수 있어요.");
        jdbc.update("DELETE FROM " + target.table + " WHERE photo_id = ?", photoId);
        String url = urls.get(0);
        if (url.startsWith(URL_PREFIX))
            jdbc.update("DELETE FROM owner_photo_file WHERE file_name = ? AND target_type = ? AND target_id = ?",
                url.substring(URL_PREFIX.length()), target.name(), id);
        return read(target, id);
    }

    private void requireMine(Target target, String loginId, long id, boolean lock) {
        long ownerId = lodgings.ownerIdOf(loginId);
        String sql = lock ? target.ownerCheck : target.ownerCheck.replace(" FOR UPDATE", "");
        Integer count = jdbc.queryForObject(sql, Integer.class, id, ownerId);
        if (count == null || count == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, target.notFound);
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
