package com.peach.scheduler.core;

/**
 * 任务处理器。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
public interface JobHandler {

    /**
     * 调度模块说明。
     *
     * <p>调度模块说明。</p>
     *
     * @param context context。
     * @return 执行结果。
     */
    JobResult execute(JobContext context);
}
