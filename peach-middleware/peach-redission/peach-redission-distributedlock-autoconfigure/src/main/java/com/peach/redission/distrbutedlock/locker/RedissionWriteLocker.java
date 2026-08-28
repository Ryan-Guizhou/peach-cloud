package com.peach.redission.distrbutedlock.locker;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;

/**
 * 写锁。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/22 11:38
 * @Description 写锁
 */
@Slf4j
public class RedissionWriteLocker implements DistributedLocker {

    private final RedissonClient redissonClient;

    public RedissionWriteLocker(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public RLock getLock(String lockKey) {
        RLock rLock = redissonClient.getReadWriteLock(lockKey).writeLock();
        rLock.lock();
        return rLock;
    }

    @Override
    public RLock lock(String lockKey) {
        RLock lock = redissonClient.getReadWriteLock(lockKey).writeLock();
        lock.lock();
        return lock;
    }

    @Override
    public RLock lock(String lockKey, long leaseTime) {
        RLock lock = redissonClient.getReadWriteLock(lockKey).writeLock();
        lock.lock(leaseTime, TimeUnit.SECONDS);
        return lock;
    }

    @Override
    public RLock lock(String lockKey, TimeUnit unit, long leaseTime) {
        RLock lock = redissonClient.getReadWriteLock(lockKey).writeLock();
        lock.lock(leaseTime, unit);
        return lock;
    }

    @Override
    public boolean tryLock(String lockKey, TimeUnit unit, long waitTime) {
        RLock lock = redissonClient.getReadWriteLock(lockKey).writeLock();
        try {
            return lock.tryLock(waitTime, unit);
        } catch (InterruptedException e) {
            log.error("Redis WriteLocker tryLock InterruptedException", e);
            return false;
        }
    }

    @Override
    public boolean tryLock(String lockKey, TimeUnit unit, long waitTime, long leaseTime) {
        RLock lock = redissonClient.getReadWriteLock(lockKey).writeLock();
        try {
            return lock.tryLock(waitTime,leaseTime,unit);
        } catch (InterruptedException e) {
            log.error("Redis WriteLocker tryLock InterruptedException", e);
            return false;
        }
    }

    @Override
    public void unlock(String lockKey) {
        RLock rLock = redissonClient.getReadWriteLock(lockKey).writeLock();
        rLock.unlock();
    }

    @Override
    public void unlock(RLock rLock) {
        rLock.unlock();
    }
}
