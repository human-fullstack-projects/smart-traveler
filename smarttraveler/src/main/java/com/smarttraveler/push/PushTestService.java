package com.smarttraveler.push;

import jakarta.annotation.PreDestroy;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Short-lived demo jobs only. Delivery is independent of the browser tab. */
@Service
public class PushTestService {
    private static final Logger log = LoggerFactory.getLogger(PushTestService.class);
    private final WebPushSender sender;
    private final ScheduledExecutorService scheduler;
    private final Clock clock;
    private final Map<String, Job> jobs = new HashMap<>();

    @Autowired
    public PushTestService(WebPushSender sender) {
        this(sender, Executors.newScheduledThreadPool(2), Clock.systemUTC());
    }

    PushTestService(WebPushSender sender, ScheduledExecutorService scheduler, Clock clock) {
        this.sender = sender;
        this.scheduler = scheduler;
        this.clock = clock;
    }

    public record Result(String id, String status, Instant scheduledAt, String message) {}
    private static class Job {
        final String id = UUID.randomUUID().toString();
        final String sessionId;
        final String endpoint;
        final Instant createdAt;
        final Instant scheduledAt;
        String status = "SCHEDULED";
        String message;
        Job(String sessionId, String endpoint, Instant now, int delay) {
            this.sessionId = sessionId;
            this.endpoint = endpoint;
            this.createdAt = now;
            this.scheduledAt = now.plusSeconds(delay);
            this.message = delay == 0 ? "알림을 발송하고 있어요." : "1분 후 알림을 예약했어요. 이제 사이트 탭을 닫아도 됩니다.";
        }
        Result result() { return new Result(id, status, scheduledAt, message); }
    }

    public Result create(String sessionId, PushSubscription subscription, int delaySeconds) {
        if (delaySeconds != 0 && delaySeconds != 60) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "즉시 또는 1분 후 알림만 테스트할 수 있습니다.");
        }
        subscription.validate();
        Job job;
        synchronized (jobs) {
            Instant now = clock.instant();
            jobs.values().removeIf(existing -> existing.createdAt.isBefore(now.minusSeconds(600)));
            if (jobs.size() >= 500) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "테스트 요청이 많습니다. 잠시 후 다시 시도해주세요.");
            boolean duplicate = jobs.values().stream().anyMatch(existing ->
                    (existing.sessionId.equals(sessionId) || existing.endpoint.equals(subscription.endpoint()))
                    && (existing.createdAt.isAfter(now.minusSeconds(3))
                        || (delaySeconds == 60 && existing.status.equals("SCHEDULED"))));
            if (duplicate) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "이미 처리 중인 알림이 있어요. 잠시 후 다시 시도해주세요.");
            job = new Job(sessionId, subscription.endpoint(), now, delaySeconds);
            jobs.put(job.id, job);
        }
        if (delaySeconds == 0) deliver(job, subscription, delaySeconds);
        else scheduler.schedule(() -> deliver(job, subscription, delaySeconds), delaySeconds, TimeUnit.SECONDS);
        synchronized (jobs) { return job.result(); }
    }

    public Result get(String sessionId, String id) {
        synchronized (jobs) {
            Job job = jobs.get(id);
            if (job == null || !job.sessionId.equals(sessionId) || job.createdAt.isBefore(clock.instant().minusSeconds(600))) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "예약 정보를 찾을 수 없어요. 서버가 재시작되었거나 예약이 만료되었을 수 있습니다.");
            }
            return job.result();
        }
    }

    private void deliver(Job job, PushSubscription subscription, int delaySeconds) {
        try {
            sender.send(subscription, job.id, delaySeconds);
            synchronized (jobs) {
                job.status = "SENT";
                job.message = "푸시 서비스에 발송했어요. 브라우저나 기기의 알림을 확인해주세요.";
            }
        } catch (Exception error) {
            synchronized (jobs) {
                job.status = error instanceof ResponseStatusException response && response.getStatusCode().value() == 410
                        ? "EXPIRED" : "FAILED";
                job.message = error instanceof ResponseStatusException response ? response.getReason() : "알림 발송에 실패했어요. 다시 시도해주세요.";
            }
            // Do not log capability URLs or subscription keys.
            log.warn("Web Push test failed: job={} type={}", job.id, error.getClass().getSimpleName());
        }
    }

    @PreDestroy
    public void close() { scheduler.shutdownNow(); }
}
