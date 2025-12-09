package com.peach.common.lock;

import lombok.Data;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @Description 分布式锁抽象类
 * @CreateTime 2025/6/20 23:36
 */
public interface DistributedLock extends Lock {

    @Override
    void unlock();

    @Override
    boolean tryLock();

    @Override
    boolean tryLock(long time, TimeUnit unit) throws InterruptedException;

    /**
     * 锁名称（业务标识）
     */
    String getName();

    /**
     * 锁类型（用于SPI查找）
     */
    String getLockType();

    /**
     * 是否支持公平锁
     */
    boolean supportsFairLock();

    /**
     * 是否支持重入
     */
    boolean supportsReentrant();

    /**
     * 获取锁信息
     */
    LockInfo getLockInfo();

    /**
     * 锁信息实体
     */
    @Data
    class LockInfo {
        private String lockKey;
        private String lockValue;
        private String lockType;
        private long acquireTime;
        private long expireTime;
        private String owner;

    }
}
