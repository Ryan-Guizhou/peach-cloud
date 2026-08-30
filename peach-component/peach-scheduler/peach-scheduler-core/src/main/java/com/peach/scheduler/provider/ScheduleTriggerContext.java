package com.peach.scheduler.provider;

import java.time.Instant;

/**
 * ScheduleTrigger上下文。
 *
 * @param jobCode 稳定任务编码
 * @param scheduledTime 计划触发时间
 * @param parameters 任务参数 JSON
 * @param providerId 调度提供方标识
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
