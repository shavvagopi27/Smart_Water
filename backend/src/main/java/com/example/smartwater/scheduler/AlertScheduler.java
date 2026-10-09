package com.example.smartwater.scheduler;

import com.example.smartwater.service.AlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AlertScheduler {
    private final AlertService alertService;

    /** Daily anomaly / threshold detection (cron: app.alerts.cron). */
    @Scheduled(cron = "${app.alerts.cron}")
    public void detect() {
        alertService.runDetection();
    }
}
