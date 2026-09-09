package com.peach.virtualthread.executor;

import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;

import java.util.List;

/**
 * 虚拟线程分组 Micrometer 指标绑定器。
 *
 * <p>指标只使用 group 低基数标签，不使用 taskId、taskName、userId、requestId 或异常信息。
 * 指标读取直接访问标量计数，不创建 GroupSnapshot，且指标值永远不参与实际并发控制。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/9 15:35
 */
public final class VirtualThreadMetricsBinder implements MeterBinder {

    private final List<DefaultVirtualExecutorService> executors;

    /**
     * 创建指标绑定器。
     *
     * @param executors 当前应用中所有静态虚拟线程业务组执行器
     */
    public VirtualThreadMetricsBinder(List<DefaultVirtualExecutorService> executors) {
        this.executors = List.copyOf(executors);
    }

    /**
     * 将虚拟线程业务组的低基数指标注册到 MeterRegistry。
     *
     * @param registry Micrometer 指标注册中心
     */
    @Override
    public void bindTo(MeterRegistry registry) {
        for (DefaultVirtualExecutorService executor : executors) {
            String group = executor.groupName();
            Gauge.builder("peach.virtual.thread.running", executor, DefaultVirtualExecutorService::runningCount)
                    .tag("group", group).register(registry);
            Gauge.builder("peach.virtual.thread.pending", executor, DefaultVirtualExecutorService::pendingCount)
                    .tag("group", group).register(registry);
            Gauge.builder("peach.virtual.thread.inflight", executor, DefaultVirtualExecutorService::inFlightCount)
                    .tag("group", group).register(registry);
            FunctionCounter.builder("peach.virtual.thread.submitted", executor, DefaultVirtualExecutorService::submittedCount)
                    .tag("group", group).register(registry);
            FunctionCounter.builder("peach.virtual.thread.completed", executor, DefaultVirtualExecutorService::completedCount)
                    .tag("group", group).register(registry);
            FunctionCounter.builder("peach.virtual.thread.failed", executor, DefaultVirtualExecutorService::failedCount)
                    .tag("group", group).register(registry);
            FunctionCounter.builder("peach.virtual.thread.cancelled", executor, DefaultVirtualExecutorService::cancelledCount)
                    .tag("group", group).register(registry);
            FunctionCounter.builder("peach.virtual.thread.rejected", executor, DefaultVirtualExecutorService::rejectedCount)
                    .tag("group", group).register(registry);
        }
    }
}
