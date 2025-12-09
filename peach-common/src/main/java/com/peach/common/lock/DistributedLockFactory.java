package com.peach.common.lock;

import java.util.Properties;

/**
 * 分布式锁工厂SPI接口
 * 通过ServiceLoader加载实现
 */
public interface DistributedLockFactory {
    
    /**
     * 工厂名称（用于SPI识别）
     */
    String getFactoryName();
    
    /**
     * 支持的锁类型
     */
    String[] getSupportedLockTypes();
    
    /**
     * 创建锁实例
     * @param lockKey 锁键
     * @param properties 锁配置
     */
    DistributedLock createLock(String lockKey, Properties properties);
    
    /**
     * 创建公平锁
     */
    default DistributedLock createFairLock(String lockKey, Properties properties) {
        throw new UnsupportedOperationException("公平锁不支持");
    }
    
    /**
     * 创建读锁
     */
    default DistributedLock createReadLock(String lockKey, Properties properties) {
        throw new UnsupportedOperationException("读锁不支持");
    }
    
    /**
     * 创建写锁
     */
    default DistributedLock createWriteLock(String lockKey, Properties properties) {
        throw new UnsupportedOperationException("写锁不支持");
    }
    
    /**
     * 初始化工厂
     */
    default void init(Properties globalProperties) {
        // 默认空实现
    }
    
    /**
     * 销毁工厂
     */
    default void destroy() {
        // 默认空实现
    }
}