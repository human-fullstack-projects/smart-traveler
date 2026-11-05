package com.smarttraveler.auth.service;

import com.smarttraveler.auth.repository.SuspensionRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SuspensionReleaseJob {
    private final SuspensionRepository suspensions;
    private final SuspensionReleaseService releases;

    // Start after ten minutes so context startup and short-lived tests do not race the sweep.
    @Scheduled(fixedDelay = 600000, initialDelay = 600000)
    public void releaseExpiredSuspensions() {
        suspensions.findByReleasedAtIsNullAndSuspendedUntilLessThanEqual(LocalDateTime.now())
            .stream().map(h -> h.getMemberId()).distinct().forEach(releases::releaseIfExpired);
    }
}
