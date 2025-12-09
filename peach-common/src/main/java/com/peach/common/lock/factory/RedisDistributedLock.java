package com.peach.common.lock.factory;


import com.peach.common.lock.DistributedLock;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.params.SetParams;

import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;

/**
 * Redis分布式锁实现
 */
public class RedisDistributedLock implements DistributedLock {
    
    private final String lockKey;
    private final String lockValue;
    private final JedisPool jedisPool;
    private final Properties config;
    private final ThreadLocal<Integer> reentrantCount = ThreadLocal.withInitial(() -> 0);
    
    public RedisDistributedLock(String lockKey, JedisPool jedisPool, Properties config) {
        this.lockKey = "lock:" + lockKey;
        this.lockValue = UUID.randomUUID().toString() + ":" + Thread.currentThread().getId();
        this.jedisPool = jedisPool;
        this.config = config;
    }

    @Override
    public void lock() {
        while (!tryLock()) {
            try {
                long retryInterval = Long.parseLong(config.getProperty("lock.retry.interval", "100"));
                TimeUnit.MILLISECONDS.sleep(retryInterval);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Lock interrupted", e);
            }
        }
    }
    
    @Override
    public boolean tryLock() {
        // 重入锁检查
        if (isReentrantLock()) {
            reentrantCount.set(reentrantCount.get() + 1);
            return true;
        }
        
        try (Jedis jedis = jedisPool.getResource()) {
            long timeout = Long.parseLong(config.getProperty("lock.timeout", "30000"));
            SetParams params = SetParams.setParams()
                .nx()  // NX: 仅当key不存在时设置
                .px(timeout);  // PX: 过期时间（毫秒）
            
            String result = jedis.set(lockKey, lockValue, params);
            boolean locked = "OK".equals(result);
            
            if (locked) {
                reentrantCount.set(1);
            }
            return locked;
        }
    }
    
    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        long deadline = System.nanoTime() + unit.toNanos(time);
        int retryCount = Integer.parseInt(config.getProperty("lock.retry.count", "3"));
        int currentRetry = 0;
        
        while (System.nanoTime() < deadline && currentRetry < retryCount) {
            if (tryLock()) {
                return true;
            }
            
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
            
            long retryInterval = Long.parseLong(config.getProperty("lock.retry.interval", "100"));
            TimeUnit.MILLISECONDS.sleep(retryInterval);
            currentRetry++;
        }
        
        return false;
    }
    
    @Override
    public void unlock() {
        // 重入锁释放
        if (reentrantCount.get() > 1) {
            reentrantCount.set(reentrantCount.get() - 1);
            return;
        }
        
        try (Jedis jedis = jedisPool.getResource()) {
            // Lua脚本保证原子性
            String luaScript =
                    "if redis.call('get', KEYS[1]) == ARGV[1] then\n" +
                            "    return redis.call('del', KEYS[1])\n" +
                            "else\n" +
                            "    return 0\n" +
                            "end";
            
            Object result = jedis.eval(luaScript, 1, lockKey, lockValue);
            if (Long.valueOf(1L).equals(result)) {
                reentrantCount.remove();
            } else {
                // 锁已过期或不是当前线程持有
                throw new IllegalMonitorStateException("Attempt to unlock lock, not locked by current thread");
            }
        }
    }
    
    @Override
    public String getName() {
        return lockKey;
    }
    
    @Override
    public String getLockType() {
        return "redis";
    }
    
    @Override
    public boolean supportsFairLock() {
        return false;  // Redis原生不支持公平锁
    }
    
    @Override
    public boolean supportsReentrant() {
        return true;
    }
    
    @Override
    public LockInfo getLockInfo() {
        LockInfo info = new LockInfo();
        info.setLockKey(lockKey);
        info.setLockValue(lockValue);
        info.setLockType("redis");
        info.setOwner(Thread.currentThread().getName());
        return info;
    }
    
    private boolean isReentrantLock() {
        try (Jedis jedis = jedisPool.getResource()) {
            String currentValue = jedis.get(lockKey);
            return lockValue.equals(currentValue);
        }
    }
    
    // Lock接口的其他方法（不支持）
    @Override
    public void lockInterruptibly() throws InterruptedException {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public Condition newCondition() {
        throw new UnsupportedOperationException();
    }
}