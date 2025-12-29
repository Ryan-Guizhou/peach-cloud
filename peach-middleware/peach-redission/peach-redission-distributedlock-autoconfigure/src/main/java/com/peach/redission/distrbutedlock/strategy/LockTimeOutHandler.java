package com.peach.redission.distrbutedlock.strategy;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/26 9:46
 */
public interface LockTimeOutHandler {

    /**
     * 处理锁超时
     * @param lockName 锁的key
     * */
    void handler(String lockName);
}
