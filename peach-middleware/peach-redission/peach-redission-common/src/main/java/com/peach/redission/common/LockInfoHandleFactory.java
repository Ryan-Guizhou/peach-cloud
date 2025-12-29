package com.peach.redission.common;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/22 10:58
 */
public class LockInfoHandleFactory implements ApplicationContextAware {

    private ApplicationContext context;
    /**
     * 获取锁信息处理类
     * @param lockInfoType
     * @return
     */
    public LockInfoHandle getLockHandle(String lockInfoType) {
        return context.getBean(lockInfoType, LockInfoHandle.class);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        context = applicationContext;
    }

}
