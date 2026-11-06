package com.smarttraveler.mypage.profile.controller;

import com.smarttraveler.mypage.profile.dto.ProfileDtos.*;
import com.smarttraveler.mypage.profile.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/mypage/profile")
public class ProfileController {
    private final ProfileService profiles;
    public ProfileController(ProfileService profiles) { this.profiles = profiles; }
    @GetMapping
    public Profile get(Authentication auth, @RequestHeader(value = "X-MyPage-Member", required = false) Long expected) {
        return profiles.get(auth, expected);
    }
    @PutMapping
    public Saved save(Authentication auth, @Valid @RequestBody SaveProfile request) { return profiles.save(auth, request); }
}
