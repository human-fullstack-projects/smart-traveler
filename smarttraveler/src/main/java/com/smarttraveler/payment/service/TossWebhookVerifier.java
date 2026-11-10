package com.smarttraveler.payment.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class TossWebhookVerifier {
    private final String securityKey;

    public TossWebhookVerifier(@Value("${app.toss.webhook-security-key:}") String securityKey) {
        this.securityKey = securityKey;
    }

    public void verify(String payload, String transmissionTime, String signature) {
        if (securityKey.isBlank() || transmissionTime == null || signature == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "웹훅 인증 정보가 없습니다.");
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(securityKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] expected = mac.doFinal((payload + ":" + transmissionTime).getBytes(StandardCharsets.UTF_8));
            for (String item : signature.split(",")) {
                String part = item.trim();
                if (!part.startsWith("v1:")) continue;
                try {
                    byte[] actual = Base64.getDecoder().decode(part.substring(3));
                    if (MessageDigest.isEqual(expected, actual)) return;
                } catch (IllegalArgumentException ignored) {
                    // A malformed candidate must not reject another valid signature.
                }
            }
        } catch (Exception error) {
            throw new IllegalStateException("웹훅 서명을 검증할 수 없습니다.", error);
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "웹훅 서명이 올바르지 않습니다.");
    }
}
