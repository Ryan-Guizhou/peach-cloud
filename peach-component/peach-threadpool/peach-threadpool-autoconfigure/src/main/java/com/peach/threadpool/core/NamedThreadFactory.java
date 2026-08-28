package com.peach.threadpool.core;

import java.util.Locale;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 命名工厂。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/5 17:54
 * @Description 命名工厂
 */
public class NamedThreadFactory implements ThreadFactory {

    private final String threadNamePrefix;

    private final boolean daemon;

    private final String poolType;

    private static final AtomicInteger THREAD_NUMBER = new AtomicInteger(1);

    public NamedThreadFactory(String threadNamePrefix,PoolType poolType ,boolean daemon) {
        this.threadNamePrefix = threadNamePrefix.endsWith("-") ? threadNamePrefix : threadNamePrefix + "-";
        this.poolType = poolType.name().toLowerCase(Locale.ROOT) + "-";
        this.daemon = daemon;
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread thread = new Thread(r);
        thread.setName(threadNamePrefix + poolType + THREAD_NUMBER.getAndIncrement());
        thread.setDaemon(daemon);
        return thread;
    }
}
