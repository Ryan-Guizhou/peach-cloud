package com.peach.redission.common;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;

import jakarta.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/19 18:53
 */
public class LocalCacheLock {

    private Cache<String, ReentrantLock> reentrantLockCache;

    @Value("${peach.localLock.durationTime:3600}")
    private long durationTime;

    @PostConstruct
    public void localLockCacheInit(){
        reentrantLockCache = Caffeine.newBuilder()
                .expireAfterWrite(durationTime, TimeUnit.HOURS)
                .build();
    }

    /**
     * 获取锁
     * @param lockKey
     * @return
     */
    public ReentrantLock getLock(String lockKey,boolean fair){
        return reentrantLockCache.get(lockKey, k -> new ReentrantLock(fair));
    }
}
