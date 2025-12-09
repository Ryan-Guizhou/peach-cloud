package com.peach.common.lock.manager;

import com.peach.common.lock.DistributedLock;
import com.peach.common.lock.DistributedLockFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 分布式锁管理器（SPI驱动）
 */
public class DistributedLockManager {
    
    private static final Map<String, DistributedLockFactory> FACTORY_MAP = new ConcurrentHashMap<>();
    private static final Properties GLOBAL_CONFIG = new Properties();
    
    static {
        // 1. 加载所有SPI实现
        ServiceLoader<DistributedLockFactory> loader = 
            ServiceLoader.load(DistributedLockFactory.class);
        
        for (DistributedLockFactory factory : loader) {
            String factoryName = factory.getFactoryName();
            FACTORY_MAP.put(factoryName, factory);
            factory.init(GLOBAL_CONFIG);
            System.out.println("Loaded lock factory: " + factoryName);
        }
        
        // 2. 加载默认配置
        loadDefaultConfig();
    }
    
    /**
     * 获取锁实例
     * @param lockType 锁类型（redis、zookeeper等）
     * @param lockKey 锁键
     * @param config 锁配置
     */
    public static DistributedLock getLock(String lockType, String lockKey, Properties config) {
        DistributedLockFactory factory = getFactory(lockType);
        if (factory == null) {
            throw new IllegalArgumentException("Unsupported lock type: " + lockType);
        }
        
        Properties mergedConfig = mergeConfig(config);
        return factory.createLock(lockKey, mergedConfig);
    }
    
    /**
     * 获取默认锁（Redis实现）
     */
    public static DistributedLock getDefaultLock(String lockKey) {
        return getLock("redis", lockKey, new Properties());
    }
    
    /**
     * 获取公平锁
     */
    public static DistributedLock getFairLock(String lockType, String lockKey, Properties config) {
        DistributedLockFactory factory = getFactory(lockType);
        if (factory == null) {
            throw new IllegalArgumentException("Unsupported lock type: " + lockType);
        }
        
        Properties mergedConfig = mergeConfig(config);
        return factory.createFairLock(lockKey, mergedConfig);
    }
    
    /**
     * 获取读写锁
     */
    public static ReadWriteLock getReadWriteLock(String lockType, String lockKey, Properties config) {
        DistributedLockFactory factory = getFactory(lockType);
        if (factory == null) {
            throw new IllegalArgumentException("Unsupported lock type: " + lockType);
        }
        
        Properties mergedConfig = mergeConfig(config);
        DistributedLock readLock = factory.createReadLock(lockKey, mergedConfig);
        DistributedLock writeLock = factory.createWriteLock(lockKey, mergedConfig);
        
        return new ReadWriteLock(readLock, writeLock);
    }
    
    /**
     * 注册自定义工厂（运行时扩展）
     */
    public static void registerFactory(DistributedLockFactory factory) {
        String factoryName = factory.getFactoryName();
        FACTORY_MAP.put(factoryName, factory);
        factory.init(GLOBAL_CONFIG);
    }
    
    /**
     * 获取所有支持的锁类型
     */
    public static Set<String> getSupportedLockTypes() {
        return FACTORY_MAP.keySet();
    }
    
    private static DistributedLockFactory getFactory(String lockType) {
        return FACTORY_MAP.get(lockType.toLowerCase());
    }
    
    private static Properties mergeConfig(Properties customConfig) {
        Properties merged = new Properties(GLOBAL_CONFIG);
        merged.putAll(customConfig);
        return merged;
    }
    
    private static void loadDefaultConfig() {
        // 从配置文件加载默认配置
        try {
            GLOBAL_CONFIG.load(DistributedLockManager.class
                .getResourceAsStream("/distlock-default.properties"));
        } catch (Exception e) {
            // 使用内置默认值
            GLOBAL_CONFIG.setProperty("lock.timeout", "30000");
            GLOBAL_CONFIG.setProperty("lock.retry.interval", "100");
            GLOBAL_CONFIG.setProperty("lock.retry.count", "3");
        }
    }
    
    /**
     * 读写锁包装类
     */
    public static class ReadWriteLock {
        private final DistributedLock readLock;
        private final DistributedLock writeLock;
        
        public ReadWriteLock(DistributedLock readLock, DistributedLock writeLock) {
            this.readLock = readLock;
            this.writeLock = writeLock;
        }
        
        public DistributedLock readLock() {
            return readLock;
        }
        
        public DistributedLock writeLock() {
            return writeLock;
        }
    }
}