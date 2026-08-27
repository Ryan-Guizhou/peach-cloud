package com.peach.redis.constant;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/19 17:51
 */
public final class MultiCacheConstant {

    private MultiCacheConstant() {
        throw new IllegalStateException("Utility class");
    }

    public static final String CACHE_MESSAGE_TOPIC = "cache-message-topic";

    public static final String REDIS_KEY_SEPARATOR = ":";

}
