package com.peach.scheduler.core;

/**
 * 任务上下文。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
public record JobContext(
        String executionId,
        String jobCode,
        String applicationName,
        String parameters,
        int attempt,
        String traceId) {
}
