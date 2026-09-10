package com.peach.scheduler.core;

/**
 * 业务任务处理器。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
public interface JobHandler {

    /**
     * 执行业务任务。
     *
     * @param context 调度执行上下文。
     * @return 任务执行结果。
     */
    JobResult execute(JobContext context);
}
