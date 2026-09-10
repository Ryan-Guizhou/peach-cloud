package com.peach.scheduler.runtime;

import org.springframework.stereotype.Indexed;

import com.peach.scheduler.core.JobHandler;
import com.peach.scheduler.core.JobRegistry;
import java.util.List;
import org.springframework.beans.factory.SmartInitializingSingleton;

/**
 * Spring 单例初始化完成后注册所有 {@link JobHandler}。
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
     * 创建任务注册初始化器。
     *
     * @param registry 任务处理器注册表。
     * @param handlers Spring 容器中的任务处理器列表。
     */
    public PeachJobRegistrationInitializer(JobRegistry registry, List<JobHandler> handlers) {
        this.registry = registry;
        this.handlers = handlers;
    }

    /**
     * 注册容器中声明的全部任务处理器。
     */
    @Override
    public void afterSingletonsInstantiated() {
        for (JobHandler handler : handlers) registry.register(handler);
    }
}
