package com.peach.scheduler.exception;

/**
 * 调度Configuration异常。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
public class SchedulerConfigurationException extends SchedulerException {

    private static final long serialVersionUID = -4077828524089181582L;

    /**
     * 创建调度配置异常。
     *
     * @param message 异常说明
     */
    public SchedulerConfigurationException(String message) {
        super(message);
    }

    /**
     * 创建带根因的调度配置异常。
     *
     * @param message 异常说明
     * @param cause 根因
     */
    public SchedulerConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
