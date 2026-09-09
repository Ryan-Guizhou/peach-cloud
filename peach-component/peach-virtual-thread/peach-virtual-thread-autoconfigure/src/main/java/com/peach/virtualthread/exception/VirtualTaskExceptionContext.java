package com.peach.virtualthread.exception;

/**
 * execute(Runnable) 未承接异常时的安全异常上下文。
 *
 * @param group 业务组
 * @param taskId 任务 ID
 * @param threadName 虚拟线程名称
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/9 15:05
 */
public record VirtualTaskExceptionContext(String group, long taskId, String threadName) {
}
