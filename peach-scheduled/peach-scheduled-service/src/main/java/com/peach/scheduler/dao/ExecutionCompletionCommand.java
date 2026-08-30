package com.peach.scheduler.dao;

import java.time.LocalDateTime;

/**
 * 执行CompletionCommand值对象。
 *
 * @param executionId 执行实例唯一标识
 * @param fromState 变更前状态
 * @param toState 变更后状态
 * @param version 乐观锁版本号
 * @param executorInstance 执行器实例标识
 * @param finishTime 实际结束时间
 * @param durationMs 执行耗时，单位毫秒
 * @param errorType 失败类型
 * @param errorMessage 脱敏后的失败摘要
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
