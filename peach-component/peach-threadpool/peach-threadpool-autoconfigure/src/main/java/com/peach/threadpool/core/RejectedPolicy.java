package com.peach.threadpool.core;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 拒绝策略枚举。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/5 18:47
 * @Description 拒绝策略枚举
 */
public enum RejectedPolicy {

    ABORT,
    CALLER_RUNS,
    DISCARD,
    DISCARD_OLDEST;

    public RejectedExecutionHandler handler() {
        return switch (this) {
            case ABORT -> new ThreadPoolExecutor.AbortPolicy();
            case CALLER_RUNS -> new ThreadPoolExecutor.CallerRunsPolicy();
            case DISCARD -> new ThreadPoolExecutor.DiscardPolicy();
            case DISCARD_OLDEST -> new ThreadPoolExecutor.DiscardOldestPolicy();
        };
    }
}
