package com.peach.scheduler.dispatch;

import com.peach.scheduler.transport.JobExecutionCommand;

/**
 * 任务Dispatcher接口。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
public interface JobDispatcher {

    /**
     * 调度模块说明。
     *
     * @param command command。
     */
    void dispatch(JobExecutionCommand command);
}
