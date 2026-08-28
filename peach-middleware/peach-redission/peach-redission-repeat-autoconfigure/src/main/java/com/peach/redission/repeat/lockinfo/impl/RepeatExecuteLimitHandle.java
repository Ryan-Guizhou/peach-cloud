package com.peach.redission.repeat.lockinfo.impl;

import com.peach.redission.common.AbstracyLockInfoHandle;

/**
 * 防重复Execute限流处理器。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/22 11:21
 */
public class RepeatExecuteLimitHandle extends AbstracyLockInfoHandle {

    public static final String LOCK_PREFIX_NAME = "REPEAT_EXECUTE_LIMIT";

    @Override
    public String getLockPrefixName() {
        return LOCK_PREFIX_NAME;
    }
}
