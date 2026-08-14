package com.peach.scheduler.core;

import com.peach.scheduler.transport.JobExecutionCommand;

/**
 * 调度运行时说明。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
public interface PeachJobExecutor {

    /**
     * 调度模块相关说明。
     *
     * @param command 参数说明
     */
    void execute(JobExecutionCommand command);
}
