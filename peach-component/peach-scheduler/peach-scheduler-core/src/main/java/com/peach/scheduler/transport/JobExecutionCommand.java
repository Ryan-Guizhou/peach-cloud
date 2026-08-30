package com.peach.scheduler.transport;

/**
 * 任务执行Command值对象。
 *
 * @param executionId 执行实例唯一标识
 * @param jobCode 稳定任务编码
 * @param applicationName 目标业务应用名称
 * @param handlerName 业务处理器名称
 * @param parameters 任务参数 JSON
 * @param timeoutMs 单次执行超时时间，单位毫秒
 * @param attempt 当前尝试次数
 * @param traceId 链路追踪标识
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
