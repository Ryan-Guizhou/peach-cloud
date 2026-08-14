package com.peach.scheduler.runtime;

import org.springframework.stereotype.Indexed;

import com.peach.scheduler.core.JobHandler;
import com.peach.scheduler.core.JobRegistry;
import java.util.List;
import org.springframework.beans.factory.SmartInitializingSingleton;

/**
 * 注册相关能力。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
@Indexed
public class PeachJobRegistrationInitializer implements SmartInitializingSingleton {
    private final JobRegistry registry;
    private final List<JobHandler> handlers;

    /**
     * 创建相关对象。
     * @param registry 参数说明
     * @param handlers 参数说明
     */
    public PeachJobRegistrationInitializer(JobRegistry registry, List<JobHandler> handlers) {
        this.registry = registry;
        this.handlers = handlers;
    }

    /**
     * 注册相关能力。
     */
    @Override
    public void afterSingletonsInstantiated() {
        for (JobHandler handler : handlers) registry.register(handler);
    }
}
