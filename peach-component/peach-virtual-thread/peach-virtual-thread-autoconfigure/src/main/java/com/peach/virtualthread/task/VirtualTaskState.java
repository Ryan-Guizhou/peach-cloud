package com.peach.virtualthread.task;

/**
 * 虚拟线程任务状态。
 *
 * <p>状态只描述 Starter 内部执行生命周期，不承载业务状态。COMPLETED、FAILED、CANCELLED
 * 为互斥终态，必须通过原子状态迁移保证只有一个终态能够胜出。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/9 14:50
 */
public enum VirtualTaskState {
    CREATED,
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELLED
}
