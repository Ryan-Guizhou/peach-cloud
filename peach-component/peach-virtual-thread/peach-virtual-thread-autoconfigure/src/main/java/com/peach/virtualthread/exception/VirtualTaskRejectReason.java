package com.peach.virtualthread.exception;

/**
 * 虚拟线程任务拒绝原因。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/9 14:40
 */
public enum VirtualTaskRejectReason {

    /**
     * 当前业务组的 Admission 容量已经耗尽。
     */
    CAPACITY_FULL,

    /**
     * BLOCK 背压等待超过配置的最大等待时间。
     */
    ACQUIRE_TIMEOUT,

    /**
     * 执行器已关闭，不再接收新任务。
     */
    EXECUTOR_SHUTDOWN,

    /**
     * BLOCK 背压等待期间提交线程被中断。
     */
    INTERRUPTED
}
