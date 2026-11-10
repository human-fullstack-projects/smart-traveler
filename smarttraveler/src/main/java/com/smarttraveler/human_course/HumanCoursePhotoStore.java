package com.smarttraveler.human_course;

import com.smarttraveler.human_course.HumanCourseDtos.Photo;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class HumanCoursePhotoStore {
    private static final int MAX_FILE_BYTES = 5 * 1024 * 1024;
    private static final int MAX_DATA_URL_LENGTH = 100 + 4 * ((MAX_FILE_BYTES + 2) / 3);
    private final Path root;

    public HumanCoursePhotoStore(@Value("${human-course.upload-dir:}") String directory) {
        root = (directory.isBlank() ? Path.of(System.getProperty("user.home"), ".smart-traveler", "human-course-photos")
            : Path.of(directory)).toAbsolutePath().normalize();
    }

    public String save(Photo photo) {
        if (photo == null || photo.name() == null || photo.name().isBlank()
                || photo.dataUrl() == null) throw badPhoto();
        int comma = photo.dataUrl().indexOf(',');
        if (comma < 0 || comma > 100 || !photo.dataUrl().startsWith("data:")
                || !photo.dataUrl().substring(0, comma).toLowerCase(Locale.ROOT).endsWith(";base64")) throw badPhoto();
        if (photo.dataUrl().length() > MAX_DATA_URL_LENGTH) throw tooLarge();
        byte[] bytes;
        try { bytes = Base64.getDecoder().decode(photo.dataUrl().substring(comma + 1)); }
        catch (IllegalArgumentException error) { throw badPhoto(); }
        if (bytes.length > MAX_FILE_BYTES) throw tooLarge();
        String extension = extension(bytes);
        if (extension == null) throw badPhoto();
        String name = UUID.randomUUID() + extension;
        try {
            Files.createDirectories(root);
            Files.write(root.resolve(name), bytes);
        } catch (IOException error) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "사진을 저장하지 못했습니다.");
        }
        return "/api/human-courses/photos/" + name;
    }

    public Resource get(String name) {
        if (!name.matches("[0-9a-f-]{36}\\.(jpg|png|webp|gif)")) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        Path file = root.resolve(name).normalize();
        if (!file.startsWith(root) || !Files.isRegularFile(file)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return new FileSystemResource(file);
    }

    private static String extension(byte[] bytes) {
        if (bytes.length >= 8 && (bytes[0] & 255) == 137 && bytes[1] == 80 && bytes[2] == 78 && bytes[3] == 71
                && bytes[4] == 13 && bytes[5] == 10 && bytes[6] == 26 && bytes[7] == 10) return ".png";
        if (bytes.length >= 3 && (bytes[0] & 255) == 255 && (bytes[1] & 255) == 216 && (bytes[2] & 255) == 255) return ".jpg";
        if (bytes.length >= 6 && bytes[0] == 71 && bytes[1] == 73 && bytes[2] == 70 && bytes[3] == 56
                && (bytes[4] == 55 || bytes[4] == 57) && bytes[5] == 97) return ".gif";
        if (bytes.length >= 12 && bytes[0] == 82 && bytes[1] == 73 && bytes[2] == 70 && bytes[3] == 70
                && bytes[8] == 87 && bytes[9] == 69 && bytes[10] == 66 && bytes[11] == 80) return ".webp";
        return null;
    }

    private static ResponseStatusException badPhoto() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "유효한 JPG·PNG·WebP·GIF 사진을 첨부해주세요.");
    }

    private static ResponseStatusException tooLarge() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "사진은 파일당 5MB, 최대 5개까지 첨부할 수 있습니다.");
    }
}
