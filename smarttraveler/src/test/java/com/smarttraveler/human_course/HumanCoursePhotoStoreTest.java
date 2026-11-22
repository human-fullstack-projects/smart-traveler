package com.smarttraveler.human_course;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.smarttraveler.human_course.HumanCourseDtos.Photo;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Base64;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.web.server.ResponseStatusException;

class HumanCoursePhotoStoreTest {
    @TempDir Path directory;

    @Test void storesSupportedImageAndRejectsInvalidData() throws Exception {
        var jdbc = mock(JdbcTemplate.class);
        var store = new HumanCoursePhotoStore(directory.toString(), jdbc);
        var image = new BufferedImage(60, 60, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < 60; y++) for (int x = 0; x < 60; x++)
            image.setRGB(x, y, (x * 1729 + y * 65537) & 0xffffff);
        var output = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(image, "png", output));
        byte[] pngBytes = output.toByteArray();
        assertTrue(pngBytes.length > 5 * 1024 && pngBytes.length < 20 * 1024);
        String png = "data:image/png;base64," + Base64.getEncoder().encodeToString(pngBytes);
        String url = store.save(new Photo("photo.PNG", png));
        String fileName = url.substring(url.lastIndexOf('/') + 1);
        verify(jdbc).update(contains("INSERT INTO human_course_photo_file"), eq(fileName), eq("image/png"), aryEq(pngBytes));
        assertFalse(Files.exists(directory.resolve(fileName)));
        doReturn(java.util.List.of(pngBytes)).when(jdbc).query(contains("SELECT image_bytes"), any(RowMapper.class), eq(fileName));
        assertArrayEquals(pngBytes, store.get(fileName).getInputStream().readAllBytes());
        String mismatchedMime = "data:application/octet-stream;base64," + Base64.getEncoder().encodeToString(pngBytes);
        assertTrue(store.save(new Photo("photo.PNG", mismatchedMime)).endsWith(".png"));
        byte[] maxBytes = Arrays.copyOf(pngBytes, 5 * 1024 * 1024);
        assertTrue(store.save(new Photo("large.png", "data:image/png;base64," + Base64.getEncoder().encodeToString(maxBytes))).endsWith(".png"));
        byte[] tooLargeBytes = Arrays.copyOf(maxBytes, maxBytes.length + 1);
        assertThrows(ResponseStatusException.class, () -> store.save(new Photo("too-large.png", "data:image/png;base64," + Base64.getEncoder().encodeToString(tooLargeBytes))));
        assertThrows(ResponseStatusException.class, () -> store.save(new Photo("bad.png", "data:image/png;base64,YQ==")));
        assertThrows(ResponseStatusException.class, () -> store.get("../other.png"));
    }

    @Test void readsLegacyLocalPhotoWhenDatabaseHasNoFile() throws Exception {
        var jdbc = mock(JdbcTemplate.class);
        var store = new HumanCoursePhotoStore(directory.toString(), jdbc);
        String fileName = "01234567-89ab-cdef-0123-456789abcdef.png";
        byte[] bytes = {1, 2, 3};
        Files.write(directory.resolve(fileName), bytes);
        assertArrayEquals(bytes, store.get(fileName).getInputStream().readAllBytes());
    }

    @Test void coverUsesOnlyPhotosThisServerCanOpen() throws Exception {
        var jdbc = mock(JdbcTemplate.class);
        var store = new HumanCoursePhotoStore(directory.toString(), jdbc);
        String inDb = "11111111-1111-1111-1111-111111111111.jpg";
        String local = "22222222-2222-2222-2222-222222222222.png";
        String missing = "33333333-3333-3333-3333-333333333333.jpg";
        Files.write(directory.resolve(local), new byte[] {1});
        doReturn(1).when(jdbc).queryForObject(contains("human_course_photo_file"), eq(Integer.class), eq(inDb));
        doReturn(0).when(jdbc).queryForObject(contains("human_course_photo_file"), eq(Integer.class), eq(local));
        doReturn(0).when(jdbc).queryForObject(contains("human_course_photo_file"), eq(Integer.class), eq(missing));
        assertTrue(store.exists("/api/human-courses/photos/" + inDb));
        assertTrue(store.exists("/api/human-courses/photos/" + local));
        assertFalse(store.exists("/api/human-courses/photos/" + missing));
        assertTrue(store.exists("https://tong.visitkorea.or.kr/a.jpg"));
        assertFalse(store.exists("/api/human-courses/photos/../x.png"));
    }
}
