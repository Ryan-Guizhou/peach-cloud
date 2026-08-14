package com.peach.scheduler.transport;

/**
 * 调度扩展接口。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
public interface ExecutionResultReporter {

    /**
     * 调度模块相关说明。
     *
     * @param event 参数说明
     */
    void report(JobExecutionResultEvent event);
}
