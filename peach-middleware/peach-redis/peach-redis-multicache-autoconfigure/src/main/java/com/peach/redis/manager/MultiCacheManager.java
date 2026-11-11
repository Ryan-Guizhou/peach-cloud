package com.peach.redis.manager;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.peach.redis.config.CaffeineCacheConfig;
import com.peach.redis.config.MultiCacheConfig;
import com.peach.redis.common.tool.RedisDao;
import com.peach.redis.common.tool.RedisDaoImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * 多语言缓存管理器。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/4 17:13
 */
@Slf4j
public class MultiCacheManager implements CacheManager {

    private final ConcurrentMap<String, Cache> cacheMap = new ConcurrentHashMap<>();

    private final Object creationLock = new Object();

    private final RedisTemplate redisTemplate;

    private final RedisDao redisDao;

    private final MultiCacheConfig cacheConfig;

    private final Set<String> cacheNames;

    public MultiCacheManager(RedisTemplate redisTemplate, MultiCacheConfig cacheConfig) {
        this(redisTemplate, cacheConfig, new RedisDaoImpl(redisTemplate));
    }

    public MultiCacheManager(RedisTemplate redisTemplate, MultiCacheConfig cacheConfig, RedisDao redisDao) {
        this.redisTemplate = Objects.requireNonNull(redisTemplate, "redisTemplate");
        this.cacheConfig = Objects.requireNonNull(cacheConfig, "cacheConfig");
        this.redisDao = Objects.requireNonNull(redisDao, "redisDao");
        this.cacheNames = cacheConfig.getCacheNames();
    }

    @Override
    public Cache getCache(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Cache name must not be blank");
        }
        Cache existing = cacheMap.get(name);
        if (existing != null) {
            return existing;
        }
        // 动态缓存名的容量校验与实例创建必须是一项不可分割的入场操作。
        synchronized (creationLock) {
            existing = cacheMap.get(name);
            if (existing != null) {
                return existing;
            }
            int maxInstances = cacheConfig.getMaxCacheInstances();
            if (maxInstances <= 0) {
                throw new IllegalArgumentException("peach.multicache.max-cache-instances must be positive");
            }
            if (cacheMap.size() >= maxInstances) {
                throw new IllegalStateException("MultiCache instance limit reached: " + maxInstances);
            }
            Cache cache = new MultiCache(name, redisTemplate, caffeineCache(), cacheConfig, redisDao);
            cacheMap.put(name, cache);
            log.debug("create cache instance, name={}", name);
            return cache;
        }
    }

    @Override
    public Collection<String> getCacheNames() {
        Set<String> names = new HashSet<>(cacheNames);
        names.addAll(cacheMap.keySet());
        return Collections.unmodifiableSet(names);
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
            case WEAK -> builder.weakKeys();
            case STRONG -> { /* Caffeine uses strong keys by default. */ }
            default -> log.error("The citation method is incorrect");
        }
    }

    /**
     * 配置 Value 引用类型
     * @param builder
     * @param strength
     * @return
     */
    private void handleValueStrength(Caffeine<Object, Object> builder, CaffeineCacheConfig.CaffeineStrength strength) {
        switch (strength) {
            case WEAK -> builder.weakValues();
            case STRONG -> { /* Caffeine uses strong values by default. */ }
            default -> log.error("The citation method is incorrect");
        }
    }

    /**
     * 清除本地缓存
     * @param cacheName 缓存名称
     * @param key 缓存键
     * @param sender 发送者
     */
    /**
     * 重新订阅后清空本 JVM 内已创建的全部 L1。
     *
     * <p>Redis Pub/Sub 不重放断线期间的消息，因此只有收到新的订阅确认，
     * 才能认为已恢复通知链路。此方法不访问 Redis L2，也不发送广播。</p>
     */
    public void clearAllLocal() {
        for (Cache cache : cacheMap.values()) {
            if (cache instanceof MultiCache multiCache) {
                multiCache.clearLocal(null);
            }
        }
    }

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
