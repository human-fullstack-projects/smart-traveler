package com.smarttraveler.push;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/push")
public class PushTestController {
    private final PushKeys keys;
    private final PushTestService service;

    public PushTestController(PushKeys keys, PushTestService service) {
        this.keys = keys;
        this.service = service;
    }

    public record TestRequest(@NotNull @Valid PushSubscription subscription, @NotNull Integer delaySeconds) {}

    @GetMapping("/public-key")
    public Map<String, String> publicKey() { return Map.of("publicKey", keys.publicKey()); }

    @PostMapping("/tests")
    public PushTestService.Result create(@Valid @RequestBody TestRequest request, HttpSession session) {
        return service.create(session.getId(), request.subscription(), request.delaySeconds());
    }

    @GetMapping("/tests/{id}")
    public PushTestService.Result get(@PathVariable String id, HttpSession session) {
        return service.get(session.getId(), id);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handle(ResponseStatusException error) {
        return ResponseEntity.status(error.getStatusCode()).body(Map.of("message", error.getReason()));
    }
}
