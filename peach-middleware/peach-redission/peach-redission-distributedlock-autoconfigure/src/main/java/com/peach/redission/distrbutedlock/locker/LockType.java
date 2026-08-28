package com.peach.redission.distrbutedlock.locker;

/**
 * 锁类型枚举。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/3/20 16:58
 */

public enum LockType {
    /**
     * 可重入锁
     */
    REENTRANT,
    /**
     * 公平锁
     */
    FAIR,
    /**
     * 读锁
     */
    READ,
    /**
     * 写锁
     */
    WRITE;

    LockType() {
    }

}
