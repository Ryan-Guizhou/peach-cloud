package com.peach.scheduler.model;

/**
 * Provider 无关的错过触发策略。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
public enum MisfirePolicy {

    /**
     * 忽略已经错过的触发时间，不进行补偿执行。
     */
    SKIP,

    /**
     * 恢复后立即补触发一次，不逐个追赶所有历史触发点。
     */
    FIRE_ONCE_NOW
}
