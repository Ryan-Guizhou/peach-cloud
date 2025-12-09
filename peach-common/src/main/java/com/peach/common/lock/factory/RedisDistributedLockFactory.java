package com.peach.common.lock.factory;


import com.peach.common.lock.DistributedLock;
import com.peach.common.lock.DistributedLockFactory;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import java.util.Properties;

/**
 * Redis锁工厂（SPI实现）
 */
public class RedisDistributedLockFactory implements DistributedLockFactory {
    
    private static final String FACTORY_NAME = "redis";
    private JedisPool jedisPool;
    
    @Override
    public String getFactoryName() {
        return FACTORY_NAME;
    }
    
    @Override
    public String[] getSupportedLockTypes() {
        return new String[]{FACTORY_NAME};
    }
    
    @Override
    public DistributedLock createLock(String lockKey, Properties properties) {
        ensureJedisPoolInitialized(properties);
        return new RedisDistributedLock(lockKey, jedisPool, properties);
    }
    
    @Override
    public void init(Properties globalProperties) {
        // 初始化时预加载连接池
        Properties merged = new Properties(globalProperties);
        merged.setProperty("redis.host", "localhost");
        merged.setProperty("redis.port", "6379");
        ensureJedisPoolInitialized(merged);
    }
    
    @Override
    public void destroy() {
        if (jedisPool != null) {
            jedisPool.close();
        }
    }
    
    private synchronized void ensureJedisPoolInitialized(Properties properties) {
        if (jedisPool == null) {
            String host = properties.getProperty("redis.host", "localhost");
            int port = Integer.parseInt(properties.getProperty("redis.port", "6379"));
            String password = "123456";
            int timeout = Integer.parseInt(properties.getProperty("redis.timeout", "2000"));
            int database = Integer.parseInt(properties.getProperty("redis.database", "0"));
            
            JedisPoolConfig poolConfig = new JedisPoolConfig();
            poolConfig.setMaxTotal(20);
            poolConfig.setMaxIdle(10);
            poolConfig.setMinIdle(5);
            poolConfig.setTestOnBorrow(true);
            
            if (password != null && !password.isEmpty()) {
                jedisPool = new JedisPool(poolConfig, host, port, timeout, password, database);
            } else {
                jedisPool = new JedisPool(poolConfig, host, port, timeout, null, database);
            }
        }
    }
}