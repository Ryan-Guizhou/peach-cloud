package com.peach.virtualthread.autoconfigure;

import com.peach.virtualthread.executor.DefaultVirtualExecutorService;
import com.peach.virtualthread.executor.VirtualThreadMetricsBinder;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import java.util.List;

/**
 * Peach 虚拟线程可选 Micrometer 自动配置。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/9 15:35
 */
@AutoConfiguration(after = PeachVirtualThreadAutoConfiguration.class)
@ConditionalOnClass({MeterRegistry.class, MeterBinder.class})
@ConditionalOnProperty(prefix = "peach.virtual-thread", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class PeachVirtualThreadMetricsAutoConfiguration {

    /**
     * 注册默认 Micrometer 指标绑定器。
     *
     * @param executorProvider 当前应用中的虚拟线程业务组执行器
     * @return 指标绑定器
     */
    @Bean
    @ConditionalOnMissingBean(VirtualThreadMetricsBinder.class)
    VirtualThreadMetricsBinder virtualThreadMetricsBinder(
            ObjectProvider<DefaultVirtualExecutorService> executorProvider) {
        List<DefaultVirtualExecutorService> executors = executorProvider.orderedStream().toList();
        return new VirtualThreadMetricsBinder(executors);
    }
}
