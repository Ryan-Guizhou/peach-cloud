package com.peach.scheduler.core;

/**
 * 任务上下文。
 *
 * @param executionId 执行实例唯一标识
 * @param jobCode 稳定任务编码
 * @param applicationName 目标业务应用名称
 * @param parameters 任务参数 JSON
 * @param attempt 当前尝试次数
 * @param traceId 链路追踪标识
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
