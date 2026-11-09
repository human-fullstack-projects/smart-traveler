package com.smarttraveler.mypage.preferences.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public final class PreferenceDtos {
    private PreferenceDtos() {}
    public record TagOption(String type, String name) {}
    public record Selection(@NotNull @Pattern(regexp = "interest|food|transport|style") String type,
        @NotBlank @Size(max = 50) String name, @NotNull @Pattern(regexp = "like|avoid") String polarity,
        @NotNull @Min(1) @Max(3) Integer weight) {}
    public record Save(@NotNull @Positive Long expectedMemberId,
        @NotNull @Pattern(regexp = "relaxed|normal|packed") String pace,
        @NotNull @Pattern(regexp = "early|normal|late") String wakeUp,
        @NotNull @Size(max = 80) List<@NotNull @Valid Selection> tags) {}
    public record Preferences(long memberId, String pace, String wakeUp, List<Selection> tags,
        List<TagOption> catalog, boolean saved) {}
}
