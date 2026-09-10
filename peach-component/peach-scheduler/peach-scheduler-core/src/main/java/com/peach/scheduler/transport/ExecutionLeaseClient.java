package com.peach.scheduler.transport;

/**
 * 调度执行租约客户端。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
public interface ExecutionLeaseClient {

    /**
     * 抢占指定执行实例的执行租约，避免多执行器重复处理同一任务。
     *
     * @param executionId 执行实例唯一标识。
     * @param executorInstance 当前执行器实例标识。
     * @return 抢占成功返回 {@code true}；已被其他实例抢占或不可执行时返回 {@code false}。
     */
    boolean claim(String executionId, String executorInstance);
}
