package com.peach.scheduler.transport;

/**
 * 调度执行结果上报接口。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
public interface ExecutionResultReporter {

    /**
     * 上报任务执行结果，供调度管理端更新执行状态、耗时和失败摘要。
     *
     * @param event 执行结果事件，错误信息必须已完成脱敏和长度控制。
     */
    void report(JobExecutionResultEvent event);
}
