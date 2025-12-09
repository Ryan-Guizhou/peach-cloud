package com.peach.redis.manager;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.peach.redis.config.CaffeineCacheConfig;
import com.peach.redis.config.MultiCacheConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/4 17:13
 */
@Slf4j
public class MultiCacheManager implements CacheManager {

    private final ConcurrentMap<String, Cache> cacheMap = new ConcurrentHashMap<>();

    private final RedisTemplate redisTemplate;

    private final MultiCacheConfig cacheConfig;

    private final Set<String> cacheNames;

    public MultiCacheManager(RedisTemplate redisTemplate,MultiCacheConfig cacheConfig) {
        super();
        this.redisTemplate = redisTemplate;
        this.cacheConfig = cacheConfig;
        this.cacheNames = cacheConfig.getCacheNames();
    }

    @Override
    public Cache getCache(String name) {
        Cache cache = cacheMap.get(name);
        if (cache != null) {
            return cache;
        }
        cache = new MultiCache(name, redisTemplate, caffeineCache(), cacheConfig);
        Cache oldCache = cacheMap.putIfAbsent(name, cache);
        log.debug("create cache instance, the cache name is : {}", name);
        return oldCache == null ? cache : oldCache;
    }

    @Override
    public Collection<String> getCacheNames() {
        return this.cacheNames;
    }

    /**
     * 创建 caffeine 缓存
     * @return
     */
    private com.github.benmanes.caffeine.cache.Cache<Object, Object> caffeineCache() {
        return Optional.ofNullable(cacheConfig)
                .map(MultiCacheConfig::getCaffeine)
                .map(config -> {

                    Caffeine<Object, Object> builder = Caffeine.newBuilder();

                    // 修正：所有时间相关配置都需要 TimeUnit 参数
                    Optional.of(config.getExpireAfterAccess()).filter(e -> e > 0)
                            .ifPresent(e -> builder.expireAfterAccess(e, TimeUnit.MILLISECONDS)); // 补充单位
                    Optional.of(config.getExpireAfterWrite()).filter(e -> e > 0)
                            .ifPresent(e -> builder.expireAfterWrite(e, TimeUnit.MILLISECONDS)); // 补充单位

                    // 这两行是正确的
                    Optional.of(config.getInitialCapacity()).filter(c -> c > 0)
                            .ifPresent(builder::initialCapacity);

                    Optional.of(config.getMaximumSize()).filter(s -> s > 0)
                            .ifPresent(builder::maximumSize);

                    Optional.ofNullable(config.getKeyStrength())
                            .ifPresent(caffeineStrength -> handleKeyStrength(builder, caffeineStrength));

                    Optional.ofNullable(config.getValueStrength())
                            .ifPresent(caffeineStrength -> handleValueStrength(builder, caffeineStrength));
                    return builder.build();
                }).orElseThrow(() -> new IllegalArgumentException("Cache configuration is unavailable"));
    }

    /**
     * 配置 Key 引用类型
     * @param builder
     * @param strength
     * @return
     */
    private void handleKeyStrength(Caffeine<Object, Object> builder, CaffeineCacheConfig.CaffeineStrength strength) {
        switch (strength) {
            case WEAK:
                builder.weakKeys();
                break;
            case STRONG:
                log.error("Caffeine does not support key soft references");
                throw new UnsupportedOperationException("Caffeine does not support key soft references");
            default:
                log.error("The citation method is incorrect");
        }
    }

    /**
     * 配置 Value 引用类型
     * @param builder
     * @param strength
     * @return
     */
    private void handleValueStrength(Caffeine<Object, Object> builder,  CaffeineCacheConfig.CaffeineStrength strength) {
        switch (strength) {
            case WEAK:
                builder.weakValues();
                break;
            case STRONG:
                builder.softValues();
                break;
            default:
                // 可记录日志或什么都不做
                log.error("The citation method is incorrect");
        }
    }

    /**
     * 清除本地缓存
     * @param cacheName 缓存名称
     * @param key 缓存键
     * @param sender 发送者
     */
    public void clearLocal(String cacheName, Object key, Integer sender) {
        Cache cache = cacheMap.get(cacheName);
        if (cache == null) {
            return;
        }
        MultiCache multiCache = (MultiCache) cache;
        if (multiCache.getLocalCache().hashCode() != sender) {
            multiCache.clearLocal(key);
        }
    }
}
