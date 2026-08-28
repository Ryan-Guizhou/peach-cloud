package com.peach.redis.stream.constant;

/**
 * Redis流常量。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/18 16:12
 */
public final class RedisStreamContant {

    private RedisStreamContant() {
        throw new IllegalStateException("Utility class");
    }

    public static final String GROUP = "group";

    public static final String BROADCAST = "broadcast";

    public static final String REDIS_STREAM_PREFIX = "peach.redis.stream";

}
