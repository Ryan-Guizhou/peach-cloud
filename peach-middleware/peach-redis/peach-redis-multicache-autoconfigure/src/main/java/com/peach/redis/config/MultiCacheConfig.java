package com.peach.redis.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.util.HashSet;
import java.util.Set;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/4 16:06
 * @Description 多级缓存配置
 */
@Data
@ConfigurationProperties(prefix = "multicache")
public class MultiCacheConfig {

    private Set<String> cacheNames = new HashSet<>();

    /**
     * 是否存储空值，默认true，防止缓存穿透
     */
    private boolean cacheNullValues = false;

    /**
     * 缓存key的前缀, 默认空字符串
     */
    private String cachePrefix;

    /**
     * 是否启用redis缓存
     */
    private boolean redisEnabled = true;

    /**
     * 是否启用caffeine缓存
     */
    private boolean caffeineEnabled = true;

    /**
     * redis 缓存配置
     */
    @NestedConfigurationProperty
    private RedisCacheConfig redis = new RedisCacheConfig();

    /**
     * caffeine 缓存配置
     */
    @NestedConfigurationProperty
    private CaffeineCacheConfig caffeine = new CaffeineCacheConfig();
}
