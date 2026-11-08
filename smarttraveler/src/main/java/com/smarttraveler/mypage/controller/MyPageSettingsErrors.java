package com.smarttraveler.mypage.controller;

import com.smarttraveler.mypage.profile.controller.ProfileController;
import com.smarttraveler.mypage.preferences.controller.PreferenceController;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice(assignableTypes = {ProfileController.class, PreferenceController.class})
public class MyPageSettingsErrors {
    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<Map<String, String>> status(ResponseStatusException error) {
        return ResponseEntity.status(error.getStatusCode()).body(Map.of("message", error.getReason() == null ? "요청을 확인해 주세요." : error.getReason()));
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, String>> validation() { return ResponseEntity.badRequest().body(Map.of("message", "입력 형식과 필수 항목을 확인해 주세요.")); }
}
