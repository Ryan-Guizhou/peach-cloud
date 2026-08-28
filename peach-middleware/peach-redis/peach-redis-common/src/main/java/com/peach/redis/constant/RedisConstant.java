package com.peach.redis.constant;

/**
 * Redis常量。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/4 17:39
 */
public final class RedisConstant {

    private RedisConstant() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 单机
     */
    public static final String STANDALONE = "standalone";
    /**
     * 哨兵模式
     */
    public static final String SENTINEL = "sentinel";
    /**
     * 集群模式
     */
    public static final String CLUSTER = "cluster";
}
