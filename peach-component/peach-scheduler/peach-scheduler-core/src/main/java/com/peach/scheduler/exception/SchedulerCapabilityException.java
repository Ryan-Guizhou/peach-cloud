package com.peach.scheduler.exception;

/**
 * 调度Capability异常。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
public class SchedulerCapabilityException extends SchedulerException {

    private static final long serialVersionUID = -4347986955631535663L;

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
