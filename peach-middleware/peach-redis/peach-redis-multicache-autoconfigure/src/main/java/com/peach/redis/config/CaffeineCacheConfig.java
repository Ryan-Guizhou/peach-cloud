package com.peach.redis.config;

import lombok.Data;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/4 16:09
 * @Description caffeine 缓存配置
 */
@Data
public class CaffeineCacheConfig {

    /**
     * 访问后过期时间
     */
    private long expireAfterAccess = 3 * 60 * 60 * 1000;

    /**
     * 写入后过期时间
     */
    private long expireAfterWrite = 3 * 60 * 60 * 1000;

    /**
     * 写入后刷新时间
     */
    private long refreshAfterWrite = 3 * 60 * 60 * 1000;

    /**
     * 初始化大小
     */
    private int initialCapacity = 500;

    /**
     * 最大缓存对象个数，超过此数量时之前放入的缓存将失效
     */
    private long maximumSize = 5000;

    /**
     * key 对象引用强度
     */
    private CaffeineStrength keyStrength;

    /**
     * value 对象引用强度
     */
    private CaffeineStrength valueStrength;

    public enum CaffeineStrength {
        /**
         * 弱引用
         */
        WEAK,

        /**
         * 强引用
         */
        STRONG
    }
}
