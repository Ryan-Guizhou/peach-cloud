package com.peach.scheduler.exception;

/**
 * 任务执行阶段异常。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
public class JobExecutionException extends SchedulerException {

    private static final long serialVersionUID = 4L;

    /**
     * 创建任务执行异常。
     *
     * @param message 异常说明
     */
    public JobExecutionException(String message) {
        super(message);
    }

    /**
     * 创建带根因的任务执行异常。
     *
     * @param message 异常说明
     * @param cause 根因
     */
    public JobExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
