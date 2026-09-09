package com.peach.virtualthread.autoconfigure;

import com.peach.virtualthread.api.VirtualExecutorService;
import com.peach.virtualthread.config.VirtualThreadProperties;
import com.peach.virtualthread.exception.DefaultVirtualTaskExceptionHandler;
import com.peach.virtualthread.exception.VirtualTaskExceptionHandler;
import com.peach.virtualthread.registry.VirtualExecutorRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.util.List;

/**
 * Peach 虚拟线程 Starter 核心自动配置。
 *
 * <p>自动配置只负责静态分组 Executor、异常处理器和关闭协调，不包含事务传播、动态容量、MDC、
 * 配置中心或跨组治理能力。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/9 15:20
 */
@AutoConfiguration
@EnableConfigurationProperties(VirtualThreadProperties.class)
@ConditionalOnProperty(prefix = "peach.virtual-thread", name = "enabled",
        havingValue = "true", matchIfMissing = true)
@Import(VirtualExecutorBeanDefinitionRegistrar.class)
public class PeachVirtualThreadAutoConfiguration {

    /**
     * 默认未承接异常处理器 Bean 名称。
     */
    public static final String EXCEPTION_HANDLER_BEAN = "peachVirtualTaskExceptionHandler";

    /**
     * 注册默认 execute(Runnable) 未承接异常处理器。
     *
     * @return 默认异常处理器
     */
    @Bean(name = EXCEPTION_HANDLER_BEAN)
    @ConditionalOnMissingBean(VirtualTaskExceptionHandler.class)
    VirtualTaskExceptionHandler peachVirtualTaskExceptionHandler() {
        return new DefaultVirtualTaskExceptionHandler();
    }

    /**
     * 注册只读执行器注册中心并协调 Spring 容器关闭。
     *
     * @param executorProvider 所有静态业务组执行器
     * @param properties Starter 配置
     * @return 执行器注册中心
     */
    @Bean
    @ConditionalOnMissingBean
    VirtualExecutorRegistry virtualExecutorRegistry(ObjectProvider<VirtualExecutorService> executorProvider,
                                                    VirtualThreadProperties properties) {
        List<VirtualExecutorService> executors = executorProvider.orderedStream().toList();
        return new VirtualExecutorRegistry(executors, properties.getShutdownAwait());
    }
}
