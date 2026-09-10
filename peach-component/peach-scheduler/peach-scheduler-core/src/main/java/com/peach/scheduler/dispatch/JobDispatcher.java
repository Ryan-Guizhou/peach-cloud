package com.peach.scheduler.dispatch;

import com.peach.scheduler.transport.JobExecutionCommand;

/**
 * 调度执行命令分发接口。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
public interface JobDispatcher {

    /**
     * 将调度触发生成的执行命令发送到执行端。
     *
     * @param command 执行命令。
     */
    void dispatch(JobExecutionCommand command);
}
