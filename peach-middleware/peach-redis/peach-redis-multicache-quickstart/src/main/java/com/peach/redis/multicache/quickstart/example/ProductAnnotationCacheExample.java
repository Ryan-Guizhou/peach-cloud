package com.peach.redis.multicache.quickstart.example;

import com.peach.redis.multicache.quickstart.domain.Product;
import com.peach.redis.multicache.quickstart.store.InMemoryProductStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Indexed;
import org.springframework.stereotype.Service;

/**
 * 注解方式：复用 MultiCache 作为 Spring CacheManager，使用 {@code @Cacheable}/{@code @CacheEvict}。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 17:40
 */
@Slf4j
@Indexed
@Service
@RequiredArgsConstructor
public class ProductAnnotationCacheExample {

    public static final String CACHE_NAME = "product-annotation";

    private final InMemoryProductStore productStore;

    /**
     * 首次调用回源并缓存；同 key 再次调用应命中多级缓存。
     */
    @Cacheable(cacheNames = CACHE_NAME, key = "#productId")
    public Product getById(String productId) {
        log.info("annotation cache miss, loading productId={}", productId);
        return productStore.findById(productId);
    }

    /**
     * 主动失效缓存，使下一次 {@link #getById(String)} 重新回源。
     */
    @CacheEvict(cacheNames = CACHE_NAME, key = "#productId")
    public void evict(String productId) {
        log.info("annotation evict, productId={}", productId);
    }
}
