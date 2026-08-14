package com.peach.scheduler.model;

/**
 * 任务调度定义类型。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
public enum ScheduleType {

    /**
     * Cron 表达式调度。
     */
    CRON,

    /**
     * 固定时间间隔调度。
     */
    FIXED_INTERVAL,

    /**
     * 仅执行一次的时间点调度。
     */
    ONE_TIME
}
