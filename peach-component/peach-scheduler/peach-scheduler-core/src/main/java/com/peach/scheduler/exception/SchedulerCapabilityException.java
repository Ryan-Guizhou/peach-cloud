package com.peach.scheduler.exception;

/**
 * 当前调度 Provider 不支持目标能力时抛出的异常。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
public class SchedulerCapabilityException extends SchedulerException {

    private static final long serialVersionUID = 3L;

    /**
     * 创建 Provider 能力异常。
     *
     * @param message 异常说明
     */
    public SchedulerCapabilityException(String message) {
        super(message);
    }

    /**
     * 创建带根因的 Provider 能力异常。
     *
     * @param message 异常说明
     * @param cause 根因
     */
    public SchedulerCapabilityException(String message, Throwable cause) {
        super(message, cause);
    }
}
