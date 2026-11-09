package com.smarttraveler.auth.controller;

import com.smarttraveler.auth.dto.SignupRequest;
import com.smarttraveler.auth.dto.SignupResult;
import com.smarttraveler.auth.service.SignupService;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class SignupController {
    private final SignupService signup;
    public SignupController(SignupService signup) { this.signup = signup; }

    @PostMapping("/signup")
    @org.springframework.web.bind.annotation.ResponseStatus(HttpStatus.CREATED)
    public SignupResult signup(@RequestBody SignupRequest request) { return signup.create(request); }

    /** Current terms for the sign-up screen (AUTH-03). */
    @org.springframework.web.bind.annotation.GetMapping("/terms")
    public List<Map<String,Object>> terms() { return signup.activeTerms(); }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> signupError(ResponseStatusException error) {
        return ResponseEntity.status(error.getStatusCode()).body(Map.of("message", error.getReason()));
    }
}
