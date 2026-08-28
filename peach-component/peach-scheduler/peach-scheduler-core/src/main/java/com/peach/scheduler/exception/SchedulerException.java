package com.peach.scheduler.exception;

/**
 * 调度异常。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
public class SchedulerException extends RuntimeException {

    private static final long serialVersionUID = 7686128735923333894L;

    /**
     * 创建调度异常。
     *
     * @param message 异常说明
     */
    public SchedulerException(String message) {
        super(message);
    }

    /**
     * 创建带根因的调度异常。
     *
     * @param message 异常说明
     * @param cause 根因
     */
    public SchedulerException(String message, Throwable cause) {
        super(message, cause);
    }
}
