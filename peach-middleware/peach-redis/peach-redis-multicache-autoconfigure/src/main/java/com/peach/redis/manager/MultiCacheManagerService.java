package com.peach.redis.manager;


import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/4 17:13
 * @Description 缓存工具类，提供统一的缓存操作方法，支持多级缓存管理
 */
public class MultiCacheManagerService {

    private MultiCacheManager cacheManager;


    public MultiCacheManagerService(MultiCacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    /**
     * 获取缓存管理器
     */
    public CacheManager getCacheManager() {
        return cacheManager;
    }

    // ==================== 基础操作方法 ====================

    /**
     * 获取缓存对象
     */
    @Nullable
    public Cache getCache(String cacheName) {
        checkCacheManager();
        return cacheManager.getCache(cacheName);
    }

    /**
     * 获取所有缓存名称
     */
    @NonNull
    public Collection<String> getCacheNames() {
        checkCacheManager();
        return cacheManager.getCacheNames();
    }

    // ==================== 缓存数据操作 ====================

    /**
     * 存入缓存
     */
    public void put(String cacheName, Object key, Object value) {
        Cache cache = getCache(cacheName);
        if (cache != null) {
            cache.put(key, value);
        }
    }

    /**
     * 批量存入缓存
     */
    public void putAll(String cacheName, Map<?, ?> data) {
        if (CollectionUtils.isEmpty(data)) {
            return;
        }
        Cache cache = getCache(cacheName);
        if (cache != null) {
            data.forEach(cache::put);
        }
    }

    /**
     * 获取缓存值
     */
    @Nullable
    public  <T> T get(String cacheName, Object key) {
        Cache cache = getCache(cacheName);
        if (cache != null) {
            Cache.ValueWrapper wrapper = cache.get(key);
            return wrapper != null ? (T) wrapper.get() : null;
        }
        return null;
    }

    /**
     * 获取缓存值，如果不存在则通过supplier获取并缓存
     */
    @Nullable
    public <T> T getOrElse(String cacheName, Object key, Supplier<T> supplier) {
        T value = get(cacheName, key);
        if (value == null && supplier != null) {
            value = supplier.get();
            if (value != null) {
                put(cacheName, key, value);
            }
        }
        return value;
    }

    /**
     * 获取缓存值，如果不存在则通过callable获取并缓存
     */
    @Nullable
    public <T> T getOrElse(String cacheName, Object key, Callable<T> callable) {
        try {
            T value = get(cacheName, key);
            if (value == null && callable != null) {
                value = callable.call();
                if (value != null) {
                    put(cacheName, key, value);
                }
            }
            return value;
        } catch (Exception e) {
            throw new RuntimeException("Failed to get or compute cache value", e);
        }
    }

    /**
     * 获取缓存值，如果不存在则通过loader获取并缓存
     */
    @Nullable
    public <T> T getOrLoad(String cacheName, Object key, Function<Object, T> loader) {
        T value = get(cacheName, key);
        if (value == null && loader != null) {
            value = loader.apply(key);
            if (value != null) {
                put(cacheName, key, value);
            }
        }
        return value;
    }

    /**
     * 删除缓存
     */
    public void evict(String cacheName, Object key) {
        Cache cache = getCache(cacheName);
        if (cache != null) {
            cache.evict(key);
        }
    }

    /**
     * 批量删除缓存
     */
    public void evict(String cacheName, Set<Object> keys) {
        if (CollectionUtils.isEmpty(keys)) {
            return;
        }
        Cache cache = getCache(cacheName);
        if (cache != null) {
            keys.forEach(cache::evict);
        }
    }

    /**
     * 清空缓存
     */
    public void clear(String cacheName) {
        Cache cache = getCache(cacheName);
        if (cache != null) {
            cache.clear();
        }
    }

    /**
     * 清空所有缓存
     */
    public void clearAll() {
        checkCacheManager();
        getCacheNames().forEach(this::clear);
    }

    // ==================== 高级操作方法 ====================

    /**
     * 如果不存在则存入缓存
     */
    public boolean putIfAbsent(String cacheName, Object key, Object value) {
        Cache cache = getCache(cacheName);
        if (cache != null) {
            Cache.ValueWrapper existing = cache.putIfAbsent(key, value);
            return existing == null;
        }
        return false;
    }

    /**
     * 获取缓存值，带默认值
     */
    @NonNull
    public <T> T getOrDefault(String cacheName, Object key, T defaultValue) {
        T value = get(cacheName, key);
        return value != null ? value : defaultValue;
    }

    /**
     * 检查缓存是否存在
     */
    public boolean contains(String cacheName, Object key) {
        return get(cacheName, key) != null;
    }


    // ==================== 便捷方法 ====================

    /**
     * 使用默认缓存名
     */
    public  void put(Object key, Object value) {
        put("default", key, value);
    }

    /**
     * 使用默认缓存名
     */
    @Nullable
    public  <T> T get(Object key) {
        return get("default", key);
    }

    /**
     * 使用默认缓存名
     */
    @NonNull
    public <T> T getOrDefault(Object key, T defaultValue) {
        return getOrDefault("default", key, defaultValue);
    }

    /**
     * 使用默认缓存名
     */
    @Nullable
    public <T> T getOrElse(Object key, Supplier<T> supplier) {
        return getOrElse("default", key, supplier);
    }

    /**
     * 使用默认缓存名
     */
    public  void evict(Object key) {
        evict("default", key);
    }

    // ==================== 配置相关 ====================

    /**
     * 检查缓存管理器是否已初始化
     */
    private  void checkCacheManager() {
        if (cacheManager == null) {
            throw new IllegalStateException("CacheManager is not initialized. " +
                    "Please call CacheUtils.setCacheManager() first.");
        }
    }

    /**
     * 预热缓存
     */
    public  void warmUp(String cacheName, Map<?, ?> warmUpData) {
        if (!CollectionUtils.isEmpty(warmUpData)) {
            putAll(cacheName, warmUpData);
        }
    }

    /**
     * 异步清除缓存（适用于大型缓存）
     */
    public  void asyncClear(String cacheName) {
        new Thread(() -> clear(cacheName), "cache-clear-thread-" + cacheName).start();
    }
}