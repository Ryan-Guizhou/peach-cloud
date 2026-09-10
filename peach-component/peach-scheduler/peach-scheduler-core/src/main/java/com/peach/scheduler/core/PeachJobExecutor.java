package com.peach.scheduler.core;

import com.peach.scheduler.transport.JobExecutionCommand;

/**
 * Peach 调度任务执行器接口。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
public interface PeachJobExecutor {

    /**
     * 执行调度命令，并负责租约检查、业务 Handler 调用和结果上报。
     *
     * @param command 执行命令。
     */
    void execute(JobExecutionCommand command);
}
