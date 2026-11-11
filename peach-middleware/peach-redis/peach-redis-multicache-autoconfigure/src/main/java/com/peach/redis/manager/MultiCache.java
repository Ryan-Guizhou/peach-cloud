package com.peach.redis.manager;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.peach.redis.constant.MultiCacheConstant;
import com.peach.redis.common.tool.RedisDao;
import com.peach.redis.listener.CacheMessage;
import com.peach.redis.listener.CacheMessageCodec;
import com.peach.redis.config.MultiCacheConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cache.support.AbstractValueAdaptingCache;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.CollectionUtils;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.stream.IntStream;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * 多级缓存。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/4 17:13
 * @Description 多级缓存
 */
@Slf4j
public class MultiCache extends AbstractValueAdaptingCache {

    // 固定数量的锁条带，避免每个历史缓存 Key 都永久保留 ReentrantLock。
    private static final int LOCK_STRIPES = 256;
    private final ReentrantLock[] keyLocks = IntStream.range(0, LOCK_STRIPES)
            .mapToObj(ignored -> new ReentrantLock())
            .toArray(ReentrantLock[]::new);

    private ReentrantLock localLockFor(Object key) {
        return keyLocks[Math.floorMod(key.hashCode(), keyLocks.length)];
    }
    /**
     * 一级缓存
     */
    private Cache<Object, Object> caffeineCache;

    /**
     * 本地缓存的实际 Redis L2 截止时间（纳秒单调时钟）。
     * 独立有界缓存，元数据丢失时仅回源，不会继续使用未知 TTL 的 L1。
     */
    private final Cache<Object, Long> l2Deadlines;
    /**
     * 二级缓存
     */
    private RedisTemplate redisTemplate;

    private final RedisDao redisDao;

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

    private final Duration randomJitter;

    protected MultiCache(String cacheName, RedisTemplate redisTemplate, Cache<Object, Object> caffeineCache, MultiCacheConfig config, RedisDao redisDao) {
        super(config.isCacheNullValues());
        this.cacheName = cacheName;
        this.redisTemplate = redisTemplate;
        this.redisDao = Objects.requireNonNull(redisDao, "redisDao");
        this.caffeineCache = caffeineCache;
        long maxTrackedKeys = Math.max(1L, config.getCaffeine().getMaximumSize());
        this.l2Deadlines = Caffeine.newBuilder().maximumSize(maxTrackedKeys).build();
        this.cachePrefix = config.getCachePrefix();
        this.defaultExpiration = config.getRedis().getDefaultExpiration();
        this.expires = config.getRedis().getExpires();
        this.randomJitter = config.getRedis().getRandomJitter();
        this.ecivtCacheTopic = config.getRedis().getTopic();
    }

    @Override
    protected Object lookup(Object key) {
        Object cacheKey = buildCacheKey(key);
        Object value = null;
        value = caffeineCache.getIfPresent(key);
        if (value != null) {
            Long deadline = l2Deadlines.getIfPresent(key);
            if (deadline != null
                    && (deadline == Long.MAX_VALUE || System.nanoTime() - deadline < 0)) {
                log.debug("get cache from caffeine, the key is : {}", cacheKey);
                return value;
            }
            // Missing TTL metadata is treated as expired. Never serve an unverified L1.
            clearLocal(key);
        }
        value = this.redisTemplate.opsForValue().get(cacheKey);
        if (value != null) {
            log.debug("get cache from redis and put in caffeine, the key is : {}", cacheKey);
            cacheLocalWithRemainingRedisTtl(key, cacheKey, value);
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
        ReentrantLock lock = localLockFor(key);
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
            throw new ValueRetrievalException(key, valueLoader, e);
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

    /**
     * 使用 Redis SET NX 保证跨实例写入原子性；本地缓存仅缓存获胜的值。
     * 返回值沿用 Spring Cache 语义：原值为 null 表示本次成功写入。
     */
    @Override
    public ValueWrapper putIfAbsent(Object key, Object value) {
        if (!super.isAllowNullValues() && value == null) {
            throw new IllegalArgumentException("Null cache value is not allowed");
        }
        Object cacheKey = buildCacheKey(key);
        Object storeValue = toStoreValue(value);
        Duration ttl = getExpire();

        Boolean inserted = ttl == null || ttl.isNegative()
                ? redisTemplate.opsForValue().setIfAbsent(cacheKey, storeValue)
                : redisTemplate.opsForValue().setIfAbsent(cacheKey, storeValue, ttl);
        if (Boolean.TRUE.equals(inserted)) {
            try {
                push(new CacheMessage(this.cacheName, key, this.caffeineCache.hashCode()));
            } finally {
                cacheLocalWithRemainingRedisTtl(key, cacheKey, storeValue);
            }
            return null;
        }
        if (inserted == null) {
            throw new IllegalStateException("Redis SET NX returned null (transaction or pipeline)");
        }
        Object previous = redisTemplate.opsForValue().get(cacheKey);
        return toValueWrapper(previous);
    }

    @Override
    public void evict(Object key) {
        // 先清除redis中缓存数据，然后清除caffeine中的缓存，
        // 避免短时间内如果先清除caffeine缓存后其他请求会再从redis里加载到caffeine中
        redisTemplate.delete(buildCacheKey(key));
        log.info("clear cache in redis, the key is : {}", buildCacheKey(key));
        try {
            push(new CacheMessage(this.cacheName, key, this.caffeineCache.hashCode()));
        } finally {
            clearLocal(key);
        }
        log.info("clear cache in caffeine, the key is : {}", key);
    }

    @Override
    public void clear() {
        // 先清除redis中缓存数据，然后清除caffeine中的缓存，
        // 避免短时间内如果先清除caffeine缓存后其他请求会再从redis里加载到caffeine中
        String pattern;
        if (StringUtils.isNotBlank(cachePrefix)) {
            pattern = cachePrefix
                    + MultiCacheConstant.REDIS_KEY_SEPARATOR
                    + this.cacheName
                    + MultiCacheConstant.REDIS_KEY_SEPARATOR
                    + "*";
        } else {
            pattern = this.cacheName
                    + MultiCacheConstant.REDIS_KEY_SEPARATOR
                    + "*";
        }
        try {
            // 删除只接受有业务前缀的模式，并受结果数量和时间预算限制。
            // 大范围超限将明确报错，而不会部分删除后假装 clear 成功。
            redisDao.deletePattern(pattern, 1000);
            push(new CacheMessage(this.cacheName, null, this.caffeineCache.hashCode()));
        } finally {
            // Redis 扫描或通知失败后也不得保留本实例原有 L1 数据。
            clearLocal(null);
        }
    }

    /**
     * @param message
     * @description 缓存变更时通知其他节点清理本地缓存
     */
    private void push(CacheMessage message) {
        byte[] payload = CacheMessageCodec.serialize(message);
        redisTemplate.execute((RedisCallback<Void>) connection -> {
            byte[] channel = redisTemplate.getStringSerializer().serialize(ecivtCacheTopic);
            connection.publish(channel, payload);
            return null;
        });
    }

    /**
     * 根据配置构建缓存key
     *
     * @param key 缓存key
     * @return
     */
    private Object buildCacheKey(Object key) {
        String rawKey = String.valueOf(key);
        if (StringUtils.isNotBlank(cachePrefix)) {
            return cachePrefix
                    + MultiCacheConstant.REDIS_KEY_SEPARATOR
                    + this.cacheName
                    + MultiCacheConstant.REDIS_KEY_SEPARATOR
                    + rawKey;
        }
        return this.cacheName
                + MultiCacheConstant.REDIS_KEY_SEPARATOR
                + rawKey;
    }

    /**
     * 从配置中获取过期时间
     * @return
     */
    private Duration getExpire() {
        Duration cacheNameExpire = expires.get(this.cacheName);
        Duration baseExpire = cacheNameExpire == null ? defaultExpiration : cacheNameExpire;
        return applyRandomJitter(baseExpire);
    }

    /**
     * 给基础过期时间增加随机抖动，避免同一时刻批量过期导致雪崩。
     *
     * @param baseExpire 基础过期时间
     * @return 叠加随机抖动后的过期时间
     */
    private Duration applyRandomJitter(Duration baseExpire) {
        if (baseExpire == null || baseExpire.isNegative() || randomJitter == null || randomJitter.isZero() || randomJitter.isNegative()) {
            return baseExpire;
        }
        long jitterMillis = randomJitter.toMillis();
        long randomMillis = ThreadLocalRandom.current().nextLong(jitterMillis + 1);
        return baseExpire.plusMillis(randomMillis);
    }

    /**
     * 向缓存中放入数据
     * @param key 缓存key
     * @param value 缓存value
     */
    private void doPut(Object key, Object value) {
        Duration expire = getExpire();
        value = toStoreValue(value);
        Object cacheKey = buildCacheKey(key);
        if (expire == null || expire.isNegative()) {
            redisTemplate.opsForValue().set(cacheKey, value);
        } else {
            redisTemplate.opsForValue().set(cacheKey, value, expire);
        }
        try {
            push(new CacheMessage(this.cacheName, key, this.caffeineCache.hashCode()));
        } finally {
            cacheLocalWithRemainingRedisTtl(key, cacheKey, value);
        }
    }

    /**
     * 仅在确知 Redis L2 仍然存在时把值放入 L1。
     *
     * <p>Redis GET 后的 PTTL 用于绑定实际剩余寿命，包括按 CacheName
     * 自定义 TTL 与随机抖动；纯 L1 命中不访问 Redis。
     * 如果 PTTL 查询失败/元数据缺失，禁止缓存不确定寿命的值。</p>
     */
    private void cacheLocalWithRemainingRedisTtl(Object key, Object redisKey, Object value) {
        Long remainingMillis;
        try {
            remainingMillis = redisTemplate.getExpire(redisKey, TimeUnit.MILLISECONDS);
        } catch (RuntimeException exception) {
            log.warn("Unable to determine Redis L2 remaining TTL; skip L1 population", exception);
            clearLocal(key);
            return;
        }
        // PTTL -1 means no expiration; -2 means the key no longer exists.
        if (remainingMillis == null || remainingMillis == 0 || remainingMillis < -1) {
            clearLocal(key);
            return;
        }
        long deadline;
        if (remainingMillis == -1) {
            deadline = Long.MAX_VALUE;
        } else {
            long span = TimeUnit.MILLISECONDS.toNanos(remainingMillis);
            long now = System.nanoTime();
            deadline = span > Long.MAX_VALUE - Math.max(0L, now)
                    ? Long.MAX_VALUE : now + span;
        }
        // Publish metadata first; losing metadata only causes a conservative cache miss.
        l2Deadlines.put(key, deadline);
        caffeineCache.put(key, value);
    }

    /**
     * @param key 本地缓存键；null 表示清空全部缓存及其 TTL 元数据
     * @description 清理本地缓存
     */
    public void clearLocal(Object key) {
        log.debug("clear local cache, the key is : {}", key);
        if (key == null) {
            caffeineCache.invalidateAll();
            l2Deadlines.invalidateAll();
        } else {
            caffeineCache.invalidate(key);
            l2Deadlines.invalidate(key);
        }
    }


    public Cache<Object, Object> getLocalCache() {
        return caffeineCache;
    }
}
