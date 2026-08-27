package com.peach.scheduler.dao;

import java.time.LocalDateTime;

/**
 * 执行实例完成状态更新参数。
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
