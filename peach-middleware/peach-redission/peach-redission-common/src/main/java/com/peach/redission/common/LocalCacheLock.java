package com.peach.redission.common;

import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.IntStream;

/**
 * 本地锁注册表使用固定条带，避免过期淘汰被持有的锁时产生同 key 两把锁。
 * 为确保锁条带的公平性语义稳定，条带统一为公平锁；不同 key 可能共享条带。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/10
 */
public class LocalCacheLock {

    private static final int LOCK_STRIPES = 256;

    private final ReentrantLock[] locks = IntStream.range(0, LOCK_STRIPES)
            .mapToObj(ignored -> new ReentrantLock(true))
            .toArray(ReentrantLock[]::new);

    /**
     * 保留旧初始化入口；条带在实例创建时初始化。
     */
    @Deprecated
    public void localLockCacheInit() {
        // No-op for compatibility with callers of the old cache initialization method.
    }

    /**
     * 获取具有稳定身份的本地互斥锁。fair 保留用于旧调用 ABI，条带始终使用公平锁。
     *
     * @param lockKey 本地资源键
     * @param fair 兼容参数
     * @return 有界锁条带
     */
    public ReentrantLock getLock(String lockKey, boolean fair) {
        Objects.requireNonNull(lockKey, "lockKey");
        return locks[Math.floorMod(lockKey.hashCode(), locks.length)];
    }
}
