package com.peach.threadpool.core;

/**
 * 线程池类型枚举。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/5 17:56
 */
public enum PoolType {
    CPU,
    IO,
    SCHEDULED,
    COMMON,
    VIRTUAL;
}
