package com.peach.scheduler.model;

/**
 * Concurrency策略。
 * <p>生产环境的最终并发事实由 Scheduler JDBC 执行记录控制，Quartz Bridge 的并发行为不作为业务并发依据。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
public enum ConcurrencyPolicy {

    /**
     * 允许同一逻辑任务存在多个并发执行实例。
     */
    ALLOW,

    /**
     * 不允许并发；新的 occurrence 会持久化等待，待前序执行结束后再分发。
     */
    DISALLOW,

    /**
     * 已存在运行中实例时跳过新的 occurrence，并记录为 {@code SKIPPED}。
     */
    SKIP_IF_RUNNING
}
