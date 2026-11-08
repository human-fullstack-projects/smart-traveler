package com.smarttraveler.mypage.preferences.controller;

import com.smarttraveler.mypage.preferences.dto.PreferenceDtos.*;
import com.smarttraveler.mypage.preferences.service.PreferenceService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/mypage/preferences")
public class PreferenceController {
    private final PreferenceService preferences;
    public PreferenceController(PreferenceService preferences) { this.preferences = preferences; }
    @GetMapping
    public Preferences get(Authentication auth, @RequestHeader(value = "X-MyPage-Member", required = false) Long expected) { return preferences.get(auth, expected); }
    @PutMapping
    public Preferences save(Authentication auth, @Valid @RequestBody Save request) { return preferences.save(auth, request); }
}
