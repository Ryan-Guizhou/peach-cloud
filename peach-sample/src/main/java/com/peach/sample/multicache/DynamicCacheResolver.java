package com.peach.sample.multicache;

import com.peach.redis.manager.MultiCacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.interceptor.CacheOperationInvocationContext;
import org.springframework.cache.interceptor.SimpleCacheResolver;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/10 15:15
 * @Description 自定义缓存解析器，根据id动态拼接缓存key，例如：userCache:123
 */
@Component("dynamicCacheResolver")
public class DynamicCacheResolver extends SimpleCacheResolver {

    public DynamicCacheResolver(@Autowired MultiCacheManager cacheManager) {
         super(cacheManager);
    }

    @Override
    public Collection<String> getCacheNames(CacheOperationInvocationContext<?> context) {
        String id = (String) context.getArgs()[0];
        List<String> caches = new ArrayList<>();
        context.getOperation().getCacheNames().forEach(cacheName -> {
            caches.add(cacheName + ":" +id);
        });
        return caches;
    }
}
