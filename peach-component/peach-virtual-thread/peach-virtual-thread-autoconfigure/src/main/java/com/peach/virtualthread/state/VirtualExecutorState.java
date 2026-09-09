package com.peach.virtualthread.state;

/**
 * 虚拟线程执行器生命周期状态。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/9 15:05
 */
public enum VirtualExecutorState {
    RUNNING,
    SHUTDOWN,
    STOP,
    TERMINATED
}
