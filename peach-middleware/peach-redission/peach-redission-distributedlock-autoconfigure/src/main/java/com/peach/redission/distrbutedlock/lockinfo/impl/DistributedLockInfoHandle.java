package com.peach.redission.distrbutedlock.lockinfo.impl;

import com.peach.redission.common.AbstractLockInfoHandle;

/**
 * Distributed锁Info处理器。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/26 11:46
 */
public class DistributedLockInfoHandle extends AbstractLockInfoHandle {

    public static final String LOCK_PREFIX_NAME = "DISTRIBUTED_LOCK";

    @Override
    public String getLockPrefixName() {
        return LOCK_PREFIX_NAME;
    }
}
