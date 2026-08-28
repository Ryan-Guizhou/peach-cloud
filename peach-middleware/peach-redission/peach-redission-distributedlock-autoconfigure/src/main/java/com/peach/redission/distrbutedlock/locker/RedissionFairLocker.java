package com.peach.redission.distrbutedlock.locker;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;

/**
 * 公平锁。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/22 11:38
 * @Description 公平锁
 */
@Slf4j
public class RedissionFairLocker implements DistributedLocker {

    private final RedissonClient redissonClient;

    public RedissionFairLocker(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public RLock getLock(String lockKey) {
        RLock rLock = redissonClient.getFairLock(lockKey);
        return rLock;
    }

    @Override
    public RLock lock(String lockKey) {
        RLock rLock = redissonClient.getFairLock(lockKey);
        rLock.lock();
        return rLock;
    }

    @Override
    public RLock lock(String lockKey, long leaseTime) {
        RLock rLock = redissonClient.getFairLock(lockKey);
        rLock.lock(leaseTime, TimeUnit.SECONDS);
        return rLock;
    }

    @Override
    public RLock lock(String lockKey, TimeUnit unit, long leaseTime) {
        RLock rLock = redissonClient.getFairLock(lockKey);
        rLock.lock(leaseTime, unit);
        return rLock;
    }

    @Override
    public boolean tryLock(String lockKey, TimeUnit unit, long waitTime) {
        RLock rLock = redissonClient.getFairLock(lockKey);
        try {
            return rLock.tryLock(waitTime, unit);
        }catch (InterruptedException e){
            log.error("Redis Fair Locker tryLock InterruptedException", e);
            return false;
        }

    }

    @Override
    public boolean tryLock(String lockKey, TimeUnit unit, long waitTime, long leaseTime) {
        RLock rLock = redissonClient.getFairLock(lockKey);
        try {
            return rLock.tryLock(waitTime,leaseTime, unit);
        }catch (InterruptedException e){
            log.error("Redis Fair Locker tryLock InterruptedException", e);
            return false;
        }
    }

    @Override
    public void unlock(String lockKey) {
        RLock rLock = redissonClient.getFairLock(lockKey);
        rLock.unlock();
    }

    @Override
    public void unlock(RLock rLock) {
        rLock.unlock();
    }
}
