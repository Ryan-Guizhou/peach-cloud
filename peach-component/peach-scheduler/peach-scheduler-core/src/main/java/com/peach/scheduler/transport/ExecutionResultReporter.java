package com.peach.scheduler.transport;

/**
 * 执行结果Reporter接口。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
public interface ExecutionResultReporter {

    /**
     * 调度模块说明。
     *
     * @param event 参数说明
     */
    void report(JobExecutionResultEvent event);
}
