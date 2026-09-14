package com.peach.redis.multicache.quickstart.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Indexed;

/**
 * 开启 Spring Cache，使 {@code @Cacheable}/{@code @CacheEvict} 走 MultiCacheManager。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 17:40
 */
@Indexed
@Configuration
@EnableCaching
public class MulticacheQuickstartConfiguration {
}
