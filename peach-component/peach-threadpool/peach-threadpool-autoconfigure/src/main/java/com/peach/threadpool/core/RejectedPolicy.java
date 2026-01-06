package com.peach.threadpool.core;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
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
        switch (this) {
            case ABORT:
                return new ThreadPoolExecutor.AbortPolicy();
            case CALLER_RUNS:
                return new ThreadPoolExecutor.CallerRunsPolicy();
            case DISCARD:
                return new ThreadPoolExecutor.DiscardPolicy();
            case DISCARD_OLDEST:
                return new ThreadPoolExecutor.DiscardOldestPolicy();
            default:
                return new ThreadPoolExecutor.CallerRunsPolicy();
        }
    }
}
