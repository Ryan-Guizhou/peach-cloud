package com.peach.scheduler.model;

/**
 * 调度Capability枚举。
 * <p>上层代码可以基于能力集合判断当前 Provider 是否支持对应控制操作，避免直接依赖具体调度引擎。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
public enum SchedulerCapability {

    /**
     * 支持运行时新增、更新或删除调度定义。
     */
    DYNAMIC_SCHEDULE,

    /**
     * 支持暂停和恢复任务。
     */
    PAUSE_RESUME,

    /**
     * 支持手工立即触发任务。
     */
    MANUAL_TRIGGER
}
