package com.smarttraveler.admin.controller;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = AdminRestController.class)
public class AdminApiErrors {
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<Map<String,String>> invalid(RuntimeException error) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message",error.getMessage()));
    }
}
