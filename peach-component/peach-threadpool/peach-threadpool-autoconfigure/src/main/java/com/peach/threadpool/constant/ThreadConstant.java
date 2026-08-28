package com.peach.threadpool.constant;

/**
 * 线程常量。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/5 17:51
 */
public final class ThreadConstant {

    private ThreadConstant() {
        throw new IllegalStateException("Utility class");
    }

    public static final String THREAD_POOL_CONFIG_PREFIX = "peach.threadpool";

    public static final String THREAD_POOL_NAME_PREFIX = "peach-pool-";

}
