package com.peach.scheduled.common;

/**
 * 一次执行记录的触发来源。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
public enum TriggerType {

    /**
     * 由 Quartz 等 Scheduling Provider 按计划触发。
     */
    SCHEDULED,

    /**
     * 由具备权限的操作人员手工触发。
     */
    MANUAL,

    /**
     * 由失败重试策略自动触发。
     */
    RETRY
}
