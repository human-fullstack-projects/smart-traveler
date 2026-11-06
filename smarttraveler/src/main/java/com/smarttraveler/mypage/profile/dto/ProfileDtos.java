package com.smarttraveler.mypage.profile.dto;

import jakarta.validation.constraints.*;

public final class ProfileDtos {
    private ProfileDtos() {}
    public record Profile(long memberId, String loginId, String nickname, String email, String avatarData) {}
    public record SaveProfile(@NotNull @Positive Long expectedMemberId,
        @NotBlank @Size(max = 50) String nickname, @NotBlank @Email @Size(max = 255) String email,
        @Size(max = 1400000) String avatarData, boolean changePassword,
        @Size(max = 200) String currentPassword, @Size(max = 200) String newPassword,
        @Size(max = 200) String passwordConfirmation) {}
    public record Saved(Profile profile, boolean passwordChanged) {}
}
