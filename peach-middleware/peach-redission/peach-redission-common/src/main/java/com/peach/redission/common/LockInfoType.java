package com.peach.redission.common;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/22 10:56
 */
public final class LockInfoType {

    private LockInfoType() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 分布式锁 Bean 名；与 {@link com.peach.redission.distrbutedlock.lockinfo.impl.DistributedLockInfoHandle} 注册一致。
     */
    public static final String DISTRIBUTE = "distributedLockInfoHandle";

    /**
     * 防重复、幂等锁 Bean 名。
     */
    public static final String REPEAT_EXECUTED = "repeatExecuted";
}
