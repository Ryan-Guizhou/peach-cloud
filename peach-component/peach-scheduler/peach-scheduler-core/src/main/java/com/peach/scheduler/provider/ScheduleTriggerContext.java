package com.peach.scheduler.provider;

import java.time.Instant;

/**
 * ScheduleTrigger上下文。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
public record ScheduleTriggerContext(
        String jobCode,
        Instant scheduledTime,
        String parameters,
        String providerId) {
}
