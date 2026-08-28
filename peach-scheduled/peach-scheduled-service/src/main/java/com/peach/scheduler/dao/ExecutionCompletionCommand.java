package com.peach.scheduler.dao;

import java.time.LocalDateTime;

/**
 * 执行CompletionCommand值对象。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/3/20 16:58
 */
public record ExecutionCompletionCommand(
        String executionId,
        String fromState,
        String toState,
        Long version,
        String executorInstance,
        LocalDateTime finishTime,
        Long durationMs,
        String errorType,
        String errorMessage) {
}
