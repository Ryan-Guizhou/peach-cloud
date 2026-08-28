package com.peach.scheduled.common;

/**
 * 执行事件。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
public enum ExecutionEvent {
    /** 将执行命令写入可靠分发通道。 */ QUEUE,
    /** 执行器原子抢占任务。 */ CLAIM,
    /** Handler 执行成功。 */ SUCCESS,
    /** Handler 执行失败并可能进入重试等待。 */ FAIL,
    /** Handler 执行超过超时时间。 */ TIMEOUT,
    /** 重试达到可重新分发条件。 */ RETRY,
    /** 重试次数已耗尽。 */ EXHAUST,
    /** 在业务执行开始前取消。 */ CANCEL,
    /** 根据调度策略跳过执行。 */ SKIP
}
