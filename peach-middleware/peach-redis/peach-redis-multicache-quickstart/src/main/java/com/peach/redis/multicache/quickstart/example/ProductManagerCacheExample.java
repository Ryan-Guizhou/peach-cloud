package com.peach.redis.multicache.quickstart.example;

import com.peach.redis.manager.MultiCache;
import com.peach.redis.multicache.quickstart.domain.Product;
import com.peach.redis.multicache.quickstart.store.InMemoryProductStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Indexed;
import org.springframework.stereotype.Service;

/**
 * CacheManager 编程方式：注入自动装配的 {@link CacheManager}，显式 put / get / 回源 / evict，
 * 并证明 L1(Caffeine) miss 后仍可命中 L2(Redis) 并回填本地。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 17:40
 */
@Slf4j
@Indexed
@Service
@RequiredArgsConstructor
public class ProductManagerCacheExample {

    public static final String CACHE_NAME = "product-manager";

    private final CacheManager cacheManager;
    private final InMemoryProductStore productStore;

    /**
     * 主动写入后读取，验证 CacheManager put/get。
     */
    public Product putThenGet(String productId) {
        Cache cache = requiredCache();
        Product product = productStore.findById(productId);
        cache.put(productId, product);
        Product cached = getProduct(cache, productId);
        log.info("cacheManager putThenGet hit, id={}, name={}", productId, cached == null ? null : cached.getName());
        return cached;
    }

    /**
     * 未命中时回源并写入缓存；再次调用应直接命中，不再回源。
     */
    public Product getOrLoad(String productId) {
        Cache cache = requiredCache();
        Product product = cache.get(productId, () -> productStore.findById(productId));
        log.info("cacheManager getOrLoad, id={}, hit={}", productId, product != null);
        return product;
    }

    /**
     * 分层证明：写入 L1+L2 后仅清本地 → 读取应命中 Redis 并回填 L1，且不回源。
     */
    public Product proveLocalMissHitsRedis(String productId) {
        Cache cache = requiredCache();
        MultiCache multiCache = asMultiCache(cache);

        Product product = productStore.findById(productId);
        cache.put(productId, product);
        productStore.resetLoadCount();

        multiCache.clearLocal(productId);
        if (multiCache.getLocalCache().getIfPresent(productId) != null) {
            throw new IllegalStateException("expected L1 miss after clearLocal");
        }

        Product restored = getProduct(cache, productId);
        if (restored == null) {
            throw new IllegalStateException("expected L2 hit from redis");
        }
        if (multiCache.getLocalCache().getIfPresent(productId) == null) {
            throw new IllegalStateException("expected L1 refill from redis");
        }
        if (productStore.loadCount() != 0) {
            throw new IllegalStateException("store must not be hit when redis still has value");
        }

        log.info("layered proof ok: L1 miss -> L2 hit -> L1 refill, id={}", productId);
        return restored;
    }

    /**
     * 按 key 失效，后续 getOrLoad 会再次回源。
     */
    public void evict(String productId) {
        requiredCache().evict(productId);
        log.info("cacheManager evict, id={}", productId);
    }

    private Cache requiredCache() {
        Cache cache = cacheManager.getCache(CACHE_NAME);
        if (cache == null) {
            throw new IllegalStateException("cache not found: " + CACHE_NAME);
        }
        return cache;
    }

    private static MultiCache asMultiCache(Cache cache) {
        if (!(cache instanceof MultiCache multiCache)) {
            throw new IllegalStateException("expected MultiCache instance, actual=" + cache.getClass().getName());
        }
        return multiCache;
    }

    private static Product getProduct(Cache cache, String productId) {
        Cache.ValueWrapper wrapper = cache.get(productId);
        if (wrapper == null) {
            return null;
        }
        return (Product) wrapper.get();
    }
}
