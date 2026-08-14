package com.peach.scheduler.dispatch;

import com.peach.scheduler.transport.JobExecutionCommand;

/**
 * 调度扩展接口。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
public interface JobDispatcher {

    /**
     * 调度模块相关说明。
     *
     * @param command 参数说明
     */
    void dispatch(JobExecutionCommand command);
}
