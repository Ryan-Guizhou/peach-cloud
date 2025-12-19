package com.peach.redis.constant;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/4 17:39
 */
public interface RedisConstant {

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
