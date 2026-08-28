package com.peach.redission.distrbutedlock.locker;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;

/**
 * 读锁。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/22 11:38
 * @Description 读锁
 */
@Slf4j
public class RedissionReadLocker implements DistributedLocker {

    private final RedissonClient redissonClient;

    public RedissionReadLocker(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public RLock getLock(String lockKey) {
        RLock rLock = redissonClient.getReadWriteLock(lockKey).readLock();
        return rLock;
    }

    @Override
    public RLock lock(String lockKey) {
        RLock rLock = redissonClient.getReadWriteLock(lockKey).readLock();
        rLock.lock();
        return rLock;
    }

    @Override
    public RLock lock(String lockKey, long leaseTime) {
        RLock rLock = redissonClient.getReadWriteLock(lockKey).readLock();
        rLock.lock(leaseTime, TimeUnit.SECONDS);
        return rLock;
    }

    @Override
    public RLock lock(String lockKey, TimeUnit unit, long leaseTime) {
        RLock rLock = redissonClient.getReadWriteLock(lockKey).readLock();
        rLock.lock(leaseTime, unit);
        return rLock;
    }

    @Override
    public boolean tryLock(String lockKey, TimeUnit unit, long waitTime) {
        RLock rLock = redissonClient.getReadWriteLock(lockKey).readLock();
        try {
           return rLock.tryLock(waitTime, unit);
        } catch (InterruptedException e) {
            log.error("Redis ReadLocker tryLock InterruptedException", e);
            return false;
        }
    }

    @Override
    public boolean tryLock(String lockKey, TimeUnit unit, long waitTime, long leaseTime) {
        RLock rLock = redissonClient.getReadWriteLock(lockKey).readLock();
        try {
            return rLock.tryLock(waitTime,leaseTime, unit);
        } catch (InterruptedException e) {
            log.error("Redis ReadLocker tryLock InterruptedException", e);
            return false;
        }
    }

    @Override
    public void unlock(String lockKey) {
        RLock rLock = redissonClient.getReadWriteLock(lockKey).readLock();
        rLock.unlock();
    }

    @Override
    public void unlock(RLock rLock) {
        rLock.unlock();
    }
}
