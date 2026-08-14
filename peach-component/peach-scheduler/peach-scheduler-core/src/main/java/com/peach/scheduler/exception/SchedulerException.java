package com.peach.scheduler.exception;

/**
 * Peach Scheduler 运行时异常基类。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
public class SchedulerException extends RuntimeException {

    private static final long serialVersionUID = 1L;

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
