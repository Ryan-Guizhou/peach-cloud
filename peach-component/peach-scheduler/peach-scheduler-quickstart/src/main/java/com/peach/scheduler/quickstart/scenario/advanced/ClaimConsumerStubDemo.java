package com.peach.scheduler.quickstart.scenario.advanced;

import com.peach.scheduler.quickstart.rocket.DemoSchedulerExecutionConsumer;
import com.peach.scheduler.quickstart.scenario.DemoLevel;
import com.peach.scheduler.quickstart.scenario.ProgressiveDemo;
import com.peach.scheduler.transport.ExecutionLeaseClient;
import com.peach.scheduler.transport.JobExecutionCommand;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Indexed;

/**
 * ADVANCED-01：说明性演示 claim 桩与 consumer 装配路径（不依赖真实 MQ 投递）。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 17:30
 */
@Slf4j
@Indexed
@Component
public class ClaimConsumerStubDemo implements ProgressiveDemo {

    private final ExecutionLeaseClient leaseClient;
    private final ObjectProvider<DemoSchedulerExecutionConsumer> consumerProvider;

    public ClaimConsumerStubDemo(ExecutionLeaseClient leaseClient,
                                 ObjectProvider<DemoSchedulerExecutionConsumer> consumerProvider) {
        this.leaseClient = leaseClient;
        this.consumerProvider = consumerProvider;
    }

    @Override
    public DemoLevel level() {
        return DemoLevel.ADVANCED;
    }

    @Override
    public int order() {
        return 1;
    }

    @Override
    public String title() {
        return "ADVANCED-01 Claim and consumer stub path";
    }

    @Override
    public void execute() {
        boolean claimed = leaseClient.claim("qs-claim-exec-1", "qs-executor-instance");
        DemoSchedulerExecutionConsumer consumer = consumerProvider.getIfAvailable();
        JobExecutionCommand command = new JobExecutionCommand(
                "qs-claim-exec-1",
                "demoCleanupJob",
                "peach-scheduler-quickstart",
                "demoCleanupJob",
                "{}",
                5_000L,
                1,
                "qs-claim-trace");
        log.info("[{}] claimed={}, consumerPresent={}, commandJobCode={}, tip=DemoSchedulerExecutionConsumer delegates to PeachJobExecutor after MQ deliver when peach.rocket.enabled=true",
                title(), claimed, consumer != null, command.jobCode());
        if (!claimed) {
            throw new IllegalStateException(title() + " self-check failed");
        }
    }
}
