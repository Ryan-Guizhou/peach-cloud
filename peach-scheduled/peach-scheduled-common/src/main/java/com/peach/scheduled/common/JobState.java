package com.peach.scheduled.common;

/**
 * 调度任务定义的标准生命周期状态。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
public enum JobState {
    /** 已创建但尚未启用。 */ DRAFT,
    /** 已启用并同步到调度引擎。 */ ENABLED,
    /** 任务定义有效，但调度引擎触发已暂停。 */ PAUSED,
    /** 已禁用并从调度引擎运行时移除。 */ DISABLED,
    /** 已逻辑删除的终态。 */ DELETED
}
