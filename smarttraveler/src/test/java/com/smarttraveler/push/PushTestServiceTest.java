package com.smarttraveler.push;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class PushTestServiceTest {
    @TempDir Path directory;
    private final WebPushSender sender = mock(WebPushSender.class);
    private final ScheduledExecutorService scheduler = mock(ScheduledExecutorService.class);
    private final Instant now = Instant.parse("2026-11-16T00:00:00Z");
    private PushTestService service;
    private PushSubscription subscription;

    @BeforeEach
    void setup() throws Exception {
        var keys = new PushKeys("", "", "mailto:push@example.com", directory.resolve("vapid.properties").toString());
        subscription = new PushSubscription("https://fcm.googleapis.com/fcm/send/test",
                new PushSubscription.Keys(keys.publicKey(), Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[16])));
        service = new PushTestService(sender, scheduler, Clock.fixed(now, ZoneOffset.UTC));
    }

    @Test
    void sendsImmediateNotificationAndKeepsResultPrivateToSession() {
        var result = service.create("session-a", subscription, 0);
        assertEquals("SENT", result.status());
        verify(sender).send(subscription, result.id(), 0);
        verifyNoInteractions(scheduler);
        assertEquals(result, service.get("session-a", result.id()));
        assertEquals(404, assertThrows(ResponseStatusException.class,
                () -> service.get("session-b", result.id())).getStatusCode().value());
    }

    @Test
    void schedulesOnServerAndDeliversWithoutAnotherBrowserRequest() {
        var result = service.create("session-a", subscription, 60);
        assertEquals("SCHEDULED", result.status());
        assertEquals(now.plusSeconds(60), result.scheduledAt());
        verifyNoInteractions(sender);
        var callback = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler).schedule(callback.capture(), eq(60L), eq(TimeUnit.SECONDS));
        callback.getValue().run();
        verify(sender).send(subscription, result.id(), 60);
        assertEquals("SENT", service.get("session-a", result.id()).status());
    }

    @Test
    void duplicateReservationAndUnsupportedDelayAreRejected() {
        assertEquals(400, assertThrows(ResponseStatusException.class,
                () -> service.create("session-a", subscription, 30)).getStatusCode().value());
        service.create("session-a", subscription, 60);
        assertEquals(429, assertThrows(ResponseStatusException.class,
                () -> service.create("session-b", subscription, 60)).getStatusCode().value());
    }

    @Test
    void expiredSubscriptionIsReportedForBrowserResubscription() {
        doThrow(new ResponseStatusException(HttpStatus.GONE, "구독 만료")).when(sender).send(any(), anyString(), anyInt());
        var result = service.create("session-a", subscription, 0);
        assertEquals("EXPIRED", result.status());
        assertEquals("구독 만료", result.message());
    }

    @Test
    void delayedFailureIsVisibleInStatus() {
        doThrow(new IllegalStateException("network unavailable")).when(sender).send(any(), anyString(), anyInt());
        var result = service.create("session-a", subscription, 60);
        var callback = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler).schedule(callback.capture(), eq(60L), eq(TimeUnit.SECONDS));
        callback.getValue().run();
        assertEquals("FAILED", service.get("session-a", result.id()).status());
    }

    @Test
    void rejectsPrivateAndSpoofedEndpointsAndInvalidKeysBeforeScheduling() {
        for (String endpoint : new String[]{"http://fcm.googleapis.com/send/test", "https://127.0.0.1/admin",
                "https://fcm.googleapis.com.evil.test/test", "https://fcm.googleapis.com:8443/test",
                "https://user@fcm.googleapis.com/test", "https://evil.test/fcm.googleapis.com"}) {
            assertThrows(ResponseStatusException.class,
                    () -> service.create("session-a", new PushSubscription(endpoint, subscription.keys()), 60));
        }
        assertThrows(ResponseStatusException.class, () -> service.create("session-a",
                new PushSubscription(subscription.endpoint(), new PushSubscription.Keys("invalid", "invalid")), 60));
        verifyNoInteractions(sender, scheduler);
    }

    @Test
    void localVapidKeysRemainStableAcrossRestarts() throws Exception {
        String file = directory.resolve("stable.properties").toString();
        var first = new PushKeys("", "", "mailto:push@example.com", file);
        var second = new PushKeys("", "", "mailto:push@example.com", file);
        assertEquals(first.publicKey(), second.publicKey());
        assertEquals(first.privateKey(), second.privateKey());
        assertThrows(IllegalStateException.class,
                () -> new PushKeys(first.publicKey(), "", "mailto:push@example.com", file));
    }
}
