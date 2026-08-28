package com.peach.scheduled.common;

/**
 * 执行State枚举。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
public enum ExecutionState {
    /** 已持久化但尚未完成可靠分发。 */ CREATED,
    /** 执行命令已可靠入队。 */ QUEUED,
    /** 已被一个执行器实例抢占。 */ RUNNING,
    /** 等待达到重试条件。 */ RETRY_WAIT,
    /** 执行成功终态。 */ SUCCEEDED,
    /** 预留失败终态。 */ FAILED,
    /** 执行超时终态。 */ TIMED_OUT,
    /** 执行取消终态。 */ CANCELLED,
    /** 执行跳过终态。 */ SKIPPED,
    /** 重试次数耗尽终态。 */ DEAD
}
