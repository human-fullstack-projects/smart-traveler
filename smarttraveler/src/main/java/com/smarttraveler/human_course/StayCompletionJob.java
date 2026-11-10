package com.smarttraveler.human_course;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class StayCompletionJob {
    private final HumanCourseService service;
    public StayCompletionJob(HumanCourseService service) { this.service = service; }

    @EventListener(ApplicationReadyEvent.class)
    public void completeOverdueOnStart() { service.completeStays(); }

    @Scheduled(cron = "0 0,30 * * * *", zone = "Asia/Seoul")
    public void completeEveryHalfHour() { service.completeStays(); }
}
