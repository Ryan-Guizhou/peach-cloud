package com.peach.redission.common;

import org.aopalliance.intercept.Joinpoint;
import org.aspectj.lang.JoinPoint;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/19 18:53
 */
public interface LockInfoHandle {

    /**
     * 获取锁名称
     * @param joinpoint
     * @param name
     * @param keys
     * @return
     */
    String getLockName(JoinPoint joinpoint, String name, String[] keys);

    /**
     * 组装锁名称
     * @param name
     * @param keys
     * @return
     */
    String getAssemblyLockName(String name,String[] keys);
}
