package com.smarttraveler.push;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.Base64;
import java.util.Locale;
import nl.martijndwars.webpush.Utils;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public record PushSubscription(@NotBlank @Size(max = 2048) String endpoint, @NotNull @Valid Keys keys) {
    public record Keys(@NotBlank @Size(max = 100) String p256dh, @NotBlank @Size(max = 32) String auth) {}

    public void validate() {
        try {
            URI uri = URI.create(endpoint);
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
            // Public test requests must never turn the server into an arbitrary HTTP client.
            boolean allowed = host.equals("fcm.googleapis.com")
                    || host.equals("updates.push.services.mozilla.com")
                    || host.endsWith(".push.services.mozilla.com")
                    || host.equals("web.push.apple.com")
                    || host.endsWith(".notify.windows.com");
            if (!allowed || !"https".equals(uri.getScheme()) || uri.getUserInfo() != null
                    || uri.getFragment() != null || (uri.getPort() != -1 && uri.getPort() != 443)) {
                throw new IllegalArgumentException();
            }
            byte[] publicKey = Base64.getUrlDecoder().decode(keys.p256dh());
            if (publicKey.length != 65 || publicKey[0] != 4
                    || Base64.getUrlDecoder().decode(keys.auth()).length != 16) {
                throw new IllegalArgumentException();
            }
            Utils.loadPublicKey(publicKey);
        } catch (Exception invalid) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "유효한 브라우저 푸시 구독이 아닙니다.");
        }
    }
}
