package com.peach.redis.manager;

import com.github.benmanes.caffeine.cache.Cache;
import com.peach.common.util.StringUtil;
import com.peach.redis.common.MultiCacheConstant;
import com.peach.redis.listener.CacheMessage;
import com.peach.redis.config.MultiCacheConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.support.AbstractValueAdaptingCache;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/4 17:13
 * @Description 多级缓存
 */
@Slf4j
public class MultiCache extends AbstractValueAdaptingCache {

    private final Map<String, ReentrantLock> keyLockMap = new ConcurrentHashMap<>();
    /**
     * 一级缓存
     */
    private Cache<Object, Object> caffeineCache;
    /**
     * 二级缓存
     */
    private RedisTemplate redisTemplate;

    /**
     * 缓存前缀
     */
    private String cachePrefix;

    /**
     * 缓存主题
     */
    private String ecivtCacheTopic;

    private String cacheName;

    private final Duration defaultExpiration;

    private final Map<String, Duration> expires;


    protected MultiCache(String cacheName, RedisTemplate redisTemplate, Cache<Object, Object> caffeineCache, MultiCacheConfig config) {
        super(config.isCacheNullValues());
        this.cacheName = cacheName;
        this.redisTemplate = redisTemplate;
        this.caffeineCache = caffeineCache;
        this.cachePrefix = config.getCachePrefix();
        this.defaultExpiration = config.getRedis().getDefaultExpiration();
        this.expires = config.getRedis().getExpires();
        this.ecivtCacheTopic = config.getRedis().getTopic();
    }

    @Override
    protected Object lookup(Object key) {
        Object cacheKey = buildCacheKey(key);
        Object value = null;
        value = caffeineCache.getIfPresent(key);
        if (value != null) {
            log.debug("get cache from caffeine, the key is : {}", cacheKey);
            return value;
        }
        value = this.redisTemplate.opsForValue().get(cacheKey);
        if (value != null) {
            log.debug("get cache from redis and put in caffeine, the key is : {}", cacheKey);
            caffeineCache.put(key, value);
        }
        return value;
    }

    @Override
    public String getName() {
        return this.cacheName;
    }

    @Override
    public Object getNativeCache() {
        return this.caffeineCache;
    }

    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        Object value = lookup(key);
        if (value != null) {
            return (T) value;
        }
        ReentrantLock lock = keyLockMap.computeIfAbsent(key.toString(), s -> {
            log.trace("create lock for key : {}", s);
            return new ReentrantLock();
        });
        lock.lock();
        try {
            value = lookup(key);
            if (value != null) {
                return (T) value;
            }
            value = valueLoader.call();
            Object storeValue = toStoreValue(value);
            put(key, storeValue);
            return (T) value;
        } catch (Exception e) {
            throw new ValueRetrievalException(key, valueLoader, e.getCause());
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void put(Object key, Object value) {
        if (!super.isAllowNullValues() && value == null) {
            this.evict(key);
            return;
        }
        doPut(key, value);
    }

    @Override
    public ValueWrapper putIfAbsent(Object key, Object value) {
        Object cacheKey = buildCacheKey(key);
        Object prevValue;
        // 考虑使用分布式锁，或者将redis的setIfAbsent改为原子性操作
        synchronized (key) {
            prevValue = redisTemplate.opsForValue().get(cacheKey);
            if (prevValue == null) {
                doPut(key, value);
            }
        }
        return toValueWrapper(prevValue);
    }


    @Override
    public void evict(Object key) {
        // 先清除redis中缓存数据，然后清除caffeine中的缓存，
        // 避免短时间内如果先清除caffeine缓存后其他请求会再从redis里加载到caffeine中
        redisTemplate.delete(buildCacheKey(key));
        push(new CacheMessage(this.cacheName, key, this.caffeineCache.hashCode()));
        caffeineCache.invalidate(key);
    }

    @Override
    public void clear() {
        push(new CacheMessage(this.cacheName, null, this.caffeineCache.hashCode()));
        caffeineCache.invalidateAll();
    }

    /**
     * @param message
     * @description 缓存变更时通知其他节点清理本地缓存
     */
    private void push(CacheMessage message) {
        redisTemplate.convertAndSend(ecivtCacheTopic, message);
    }

    /**
     * 根据配置构建缓存key
     *
     * @param key 缓存key
     * @return
     */
    private Object buildCacheKey(Object key) {
        return Optional.ofNullable(cachePrefix)
                .filter(StringUtil::isNotBlank)
                .map(prefix -> prefix + MultiCacheConstant.REDIS_KEY_SEPARATOR + key)
                .orElse(String.valueOf(key));
    }

    /**
     * 从配置中获取过期时间
     * @return
     */
    private Duration getExpire() {
        Duration cacheNameExpire = expires.get(this.cacheName);
        return cacheNameExpire == null ? defaultExpiration : cacheNameExpire;
    }

    /**
     * 向缓存中放入数据
     * @param key 缓存key
     * @param value 缓存value
     */
    private void doPut(Object key, Object value) {
        Duration expire = getExpire();
        value = toStoreValue(value);
        if (!expire.isNegative()) {
            redisTemplate.opsForValue().set(buildCacheKey(key), value, expire);
        } else {
            redisTemplate.opsForValue().set(buildCacheKey(key), value);
        }
        push(new CacheMessage(this.cacheName, key, this.caffeineCache.hashCode()));
        caffeineCache.put(key, value);
    }


    /**
     * @param key
     * @description 清理本地缓存
     */
    public void clearLocal(Object key) {
        log.debug("clear local cache, the key is : {}", key);
        if (key == null) {
            caffeineCache.invalidateAll();
        } else {
            caffeineCache.invalidate(key);
        }
    }


    public Cache<Object, Object> getLocalCache() {
        return caffeineCache;
    }
}
