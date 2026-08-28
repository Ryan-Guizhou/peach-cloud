package com.peach.scheduler.core;

import com.peach.scheduler.transport.JobExecutionCommand;

/**
 * Peach任务Executor接口。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
public interface PeachJobExecutor {

    /**
     * 调度模块说明。
     *
     * @param command command。
     */
    void execute(JobExecutionCommand command);
}
