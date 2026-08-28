package com.peach.scheduler.transport;

/**
 * 任务执行Command值对象。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
public record JobExecutionCommand(
        String executionId,
        String jobCode,
        String applicationName,
        String handlerName,
        String parameters,
        long timeoutMs,
        int attempt,
        String traceId) {
}
