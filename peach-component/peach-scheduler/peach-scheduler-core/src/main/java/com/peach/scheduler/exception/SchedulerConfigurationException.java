package com.peach.scheduler.exception;

/**
 * 调度配置、任务定义或 Provider 注册不合法时抛出的异常。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
public class SchedulerConfigurationException extends SchedulerException {

    private static final long serialVersionUID = 2L;

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
