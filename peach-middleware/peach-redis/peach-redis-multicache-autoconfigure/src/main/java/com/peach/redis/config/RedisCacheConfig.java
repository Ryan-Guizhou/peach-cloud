package com.peach.redis.config;

import com.peach.redis.common.MultiCacheConstant;
import lombok.Data;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/4 16:08
 * @Description redis 缓存配置
 */
@Data
public class RedisCacheConfig {
    /**
     * 全局过期时间，默认不过期
     */
    private Duration defaultExpiration = Duration.ofHours(6);

    /**
     * 每个cacheName的过期时间，优先级比defaultExpiration高
     */
    private Map<String, Duration> expires = new HashMap<>();

    /**
     * 缓存更新时通知其他节点的topic名称
     */
    private String topic = MultiCacheConstant.CACHE_MESSAGE_TOPIC;
}
