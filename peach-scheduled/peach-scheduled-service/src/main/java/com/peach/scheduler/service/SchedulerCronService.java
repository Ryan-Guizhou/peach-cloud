package com.peach.scheduler.service;

import com.peach.scheduler.service.internal.QuartzCronPreviewBridge;
import org.springframework.stereotype.Indexed;

import java.time.Instant;
import java.util.List;

@Indexed
public class SchedulerCronService {

    public SchedulerCronService() {
        // Intentionally empty.
    }

    public List<Instant> preview(String expression, String timeZone, int count) {
        try {
            int limit = Math.clamp(count, 1, 20);
            return QuartzCronPreviewBridge.preview(expression, timeZone, limit);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid Quartz cron expression", ex);
        }
    }
}
