package com.peach.setting.config;

import com.peach.redis.manager.MultiCacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.interceptor.CacheOperationInvocationContext;
import org.springframework.cache.interceptor.SimpleCacheResolver;
import org.springframework.stereotype.Component;

import java.util.Collection;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/7 12:20
 * @Description setting 模块缓存解析器
 */
@Component("settingCacheResolver")
public class SettingCacheResolver extends SimpleCacheResolver {

    public SettingCacheResolver(@Autowired MultiCacheManager cacheManager) {
        super(cacheManager);
    }

    @Override
    public Collection<String> getCacheNames(CacheOperationInvocationContext<?> context) {
        return super.getCacheNames(context);
    }
}

