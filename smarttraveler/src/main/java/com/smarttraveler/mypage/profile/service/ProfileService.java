package com.smarttraveler.mypage.profile.service;

import com.smarttraveler.mypage.profile.dto.ProfileDtos.*;
import com.smarttraveler.mypage.profile.repository.ProfileRepository;
import com.smarttraveler.mypage.service.MyPageService;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.imageio.ImageIO;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class ProfileService {
    private final MyPageService owners;
    private final ProfileRepository profiles;
    private final PasswordEncoder passwords;
    public ProfileService(MyPageService owners, ProfileRepository profiles, PasswordEncoder passwords) {
        this.owners = owners; this.profiles = profiles; this.passwords = passwords;
    }
    public Profile get(Authentication auth, Long expected) { return profiles.find(owners.owner(auth, expected)); }

    @Transactional
    public Saved save(Authentication auth, SaveProfile request) {
        long owner = owners.owner(auth, request.expectedMemberId());
        String avatar = normalizeImage(request.avatarData());
        String existingPassword = profiles.lockPassword(owner);
        if (existingPassword == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "다시 로그인해 주세요.");
        String encoded = null;
        if (request.changePassword()) {
            if (request.currentPassword() == null || request.currentPassword().getBytes(StandardCharsets.UTF_8).length > 72
                || !passwords.matches(request.currentPassword(), existingPassword)) {
                throw bad("현재 비밀번호가 맞지 않습니다.");
            }
            String next = request.newPassword();
            if (next == null || next.length() < 8 || next.getBytes(StandardCharsets.UTF_8).length > 72
                || !next.matches("(?s).*[A-Za-z].*") || !next.matches("(?s).*[0-9].*")) {
                throw bad("새 비밀번호는 영문·숫자를 포함한 8자 이상, UTF-8 기준 72바이트 이하여야 합니다.");
            }
            if (!next.equals(request.passwordConfirmation())) throw bad("새 비밀번호 확인이 일치하지 않습니다.");
            if (passwords.matches(next, existingPassword)) throw bad("현재 비밀번호와 다른 비밀번호를 입력해 주세요.");
            encoded = passwords.encode(next);
        }
        try {
            profiles.update(owner, request.nickname().trim(), request.email().trim(), encoded, avatar);
        } catch (DuplicateKeyException conflict) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다.");
        }
        return new Saved(profiles.find(owner), request.changePassword());
    }

    // Data URL을 그대로 신뢰하지 않고 크기·실제 이미지 형식을 검사하고 PNG로 재인코딩한다.
    String normalizeImage(String source) {
        if (source == null || source.isBlank()) return null;
        if (!source.startsWith("data:image/png;base64,") && !source.startsWith("data:image/jpeg;base64,")) throw bad("PNG 또는 JPEG 프로필 이미지만 저장할 수 있습니다.");
        try {
            byte[] bytes = Base64.getDecoder().decode(source.substring(source.indexOf(',') + 1));
            if (bytes.length > 1024 * 1024) throw bad("프로필 이미지는 1MB 이하여야 합니다.");
            try (var stream = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
                var readers = ImageIO.getImageReaders(stream);
                if (!readers.hasNext()) throw bad("이미지를 읽을 수 없습니다.");
                var reader = readers.next();
                try {
                    reader.setInput(stream);
                    String format = reader.getFormatName();
                    if (!format.equalsIgnoreCase("PNG") && !format.equalsIgnoreCase("JPEG")) throw bad("지원하지 않는 이미지입니다.");
                    if (reader.getWidth(0) > 512 || reader.getHeight(0) > 512) throw bad("프로필 이미지는 가로·세로 512픽셀 이하여야 합니다.");
                    var image = reader.read(0);
                    var output = new ByteArrayOutputStream();
                    if (!ImageIO.write(image, "png", output) || output.size() > 1024 * 1024) throw bad("이미지 용량을 줄여 주세요.");
                    return "data:image/png;base64," + Base64.getEncoder().encodeToString(output.toByteArray());
                } finally { reader.dispose(); }
            }
        } catch (ResponseStatusException ex) { throw ex; }
        catch (Exception invalid) { throw bad("올바른 프로필 이미지를 선택해 주세요."); }
    }
    private ResponseStatusException bad(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
}
