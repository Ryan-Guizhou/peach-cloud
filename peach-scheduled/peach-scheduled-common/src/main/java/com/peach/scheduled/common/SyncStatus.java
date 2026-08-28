package com.peach.scheduled.common;

/**
 * SyncStatus枚举。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
public enum SyncStatus {

    /**
     * Provider 运行时状态与 JDBC 中的期望定义一致。
     */
    SYNCED,

    /**
     * 任务定义已经变更，等待 Reconciler 同步到 Provider。
     */
    PENDING,

    /**
     * 最近一次同步失败，需要后续自动重试或人工排障。
     */
    FAILED
}
