package com.smarttraveler.human_course;

import static org.junit.jupiter.api.Assertions.*;

import com.smarttraveler.human_course.HumanCourseDtos.Photo;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Base64;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.web.server.ResponseStatusException;

class HumanCoursePhotoStoreTest {
    @TempDir Path directory;

    @Test void storesSupportedImageAndRejectsInvalidData() throws Exception {
        var store = new HumanCoursePhotoStore(directory.toString());
        var image = new BufferedImage(60, 60, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < 60; y++) for (int x = 0; x < 60; x++)
            image.setRGB(x, y, (x * 1729 + y * 65537) & 0xffffff);
        var output = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(image, "png", output));
        byte[] pngBytes = output.toByteArray();
        assertTrue(pngBytes.length > 5 * 1024 && pngBytes.length < 20 * 1024);
        String png = "data:image/png;base64," + Base64.getEncoder().encodeToString(pngBytes);
        String url = store.save(new Photo("photo.PNG", png));
        assertTrue(store.get(url.substring(url.lastIndexOf('/') + 1)).exists());
        String mismatchedMime = "data:application/octet-stream;base64," + Base64.getEncoder().encodeToString(pngBytes);
        assertTrue(store.save(new Photo("photo.PNG", mismatchedMime)).endsWith(".png"));
        byte[] maxBytes = Arrays.copyOf(pngBytes, 5 * 1024 * 1024);
        assertTrue(store.save(new Photo("large.png", "data:image/png;base64," + Base64.getEncoder().encodeToString(maxBytes))).endsWith(".png"));
        byte[] tooLargeBytes = Arrays.copyOf(maxBytes, maxBytes.length + 1);
        assertThrows(ResponseStatusException.class, () -> store.save(new Photo("too-large.png", "data:image/png;base64," + Base64.getEncoder().encodeToString(tooLargeBytes))));
        assertThrows(ResponseStatusException.class, () -> store.save(new Photo("bad.png", "data:image/png;base64,YQ==")));
        assertThrows(ResponseStatusException.class, () -> store.get("../other.png"));
    }
}
