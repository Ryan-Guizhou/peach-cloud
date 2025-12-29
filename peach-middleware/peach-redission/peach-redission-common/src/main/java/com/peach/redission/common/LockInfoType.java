package com.peach.redission.common;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/22 10:56
 */
public interface LockInfoType {

    /**
     * 分布式锁
     */
    String DISTRIBUTE = "DISTRIBUTED";

    /**
     * 防重复、幂等锁
     */
    String REPEAT_EXCUTED = "REPEAT_EXCUTED";
}
