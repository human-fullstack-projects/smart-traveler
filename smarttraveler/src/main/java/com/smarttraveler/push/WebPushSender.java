package com.smarttraveler.push;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.util.Map;
import nl.martijndwars.webpush.AbstractPushService;
import nl.martijndwars.webpush.Encoding;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.Urgency;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

@Component
public class WebPushSender extends AbstractPushService<WebPushSender> {
    private final ObjectMapper mapper;
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).followRedirects(HttpClient.Redirect.NEVER).build();

    public WebPushSender(PushKeys keys, ObjectMapper mapper) throws GeneralSecurityException {
        super(keys.publicKey(), keys.privateKey(), keys.subject());
        this.mapper = mapper;
    }

    public void send(PushSubscription subscription, String id, int delaySeconds) {
        try {
            String body = delaySeconds == 0 ? "즉시 알림이 도착했어요! 브라우저 푸시가 정상 작동합니다."
                    : "1분 전에 예약한 알림이에요. 사이트 탭을 닫아도 받을 수 있어요.";
            var notification = Notification.builder().endpoint(subscription.endpoint())
                    .userPublicKey(subscription.keys().p256dh()).userAuth(subscription.keys().auth())
                    .payload(mapper.writeValueAsString(Map.of("title", "스마트 트래블 알림 테스트",
                            "body", body, "tag", id)))
                    .ttl(300).urgency(Urgency.HIGH).build();
            var prepared = prepareRequest(notification, Encoding.AES128GCM);
            var request = HttpRequest.newBuilder(URI.create(prepared.getUrl()))
                    .timeout(Duration.ofSeconds(15)).POST(HttpRequest.BodyPublishers.ofByteArray(prepared.getBody()));
            prepared.getHeaders().forEach(request::header);
            int status = client.send(request.build(), HttpResponse.BodyHandlers.discarding()).statusCode();
            if (status == 404 || status == 410) {
                throw new ResponseStatusException(HttpStatus.GONE, "알림 구독이 만료되었습니다. 다시 시도해주세요.");
            }
            if (status < 200 || status >= 300) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "푸시 서비스가 발송을 거절했습니다. 잠시 후 다시 시도해주세요.");
            }
        } catch (ResponseStatusException error) {
            throw error;
        } catch (Exception error) {
            if (error instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "푸시 서비스에 연결하지 못했습니다. 잠시 후 다시 시도해주세요.");
        }
    }
}
