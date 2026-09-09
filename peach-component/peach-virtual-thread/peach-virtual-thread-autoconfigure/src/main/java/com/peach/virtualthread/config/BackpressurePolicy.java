package com.peach.virtualthread.config;

/**
 * 虚拟线程任务背压策略。
 *
 * <p>Starter 只保留两种可解释的策略：容量耗尽时立即拒绝，或在调用线程上进行有界等待。
 * 不提供 CallerRuns、Discard 等策略，避免把阻塞任务意外带回请求线程或静默丢失任务。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/9 14:30
 */
public enum BackpressurePolicy {

    /**
     * 容量耗尽后立即拒绝任务。
     */
    REJECT,

    /**
     * 容量耗尽后在调用线程上最多等待 acquireTimeout 指定的时间。
     */
    BLOCK
}
