package com.peach.redis.common;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/4 17:39
 */
public interface MultiCacheConstant {

    String CACHE_MESSAGE_TOPIC = "cache-message-topic";

    String REDIS_KEY_SEPARATOR = ":";

    /**
     * 单机
     */
    String STANDALONE = "standalone";
    /**
     * 哨兵模式
     */
    String SENTINEL = "sentinel";
    /**
     * 集群模式
     */
    String CLUSTER = "cluster";
}
