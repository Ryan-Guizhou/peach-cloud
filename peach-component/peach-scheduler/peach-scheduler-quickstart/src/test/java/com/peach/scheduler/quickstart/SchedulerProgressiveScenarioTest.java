package com.peach.scheduler.quickstart;

import com.peach.scheduler.quickstart.job.DemoCleanupJob;
import com.peach.scheduler.quickstart.rocket.DemoSchedulerExecutionConsumer;
import com.peach.scheduler.quickstart.scenario.advanced.ClaimConsumerStubDemo;
import com.peach.scheduler.quickstart.scenario.basic.JobHandlerExecuteDemo;
import com.peach.scheduler.quickstart.scenario.intermediate.DemoRunnerTriggerDemo;
import com.peach.scheduler.transport.ExecutionLeaseClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Scheduler 递进演示的轻量行为测试（不拉起完整 Rocket 装配）。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 17:30
 */
class SchedulerProgressiveScenarioTest {

    @Test
    void shouldExecuteBasicAndIntermediateJobHandlerDemos() {
        DemoCleanupJob job = new DemoCleanupJob();
        assertThatCode(() -> new JobHandlerExecuteDemo(job).execute()).doesNotThrowAnyException();
        assertThatCode(() -> new DemoRunnerTriggerDemo(job).execute()).doesNotThrowAnyException();
    }

    @Test
    void shouldExecuteAdvancedClaimStubDemo() {
        ExecutionLeaseClient leaseClient = mock(ExecutionLeaseClient.class);
        when(leaseClient.claim(anyString(), anyString())).thenReturn(true);
        @SuppressWarnings("unchecked")
        ObjectProvider<DemoSchedulerExecutionConsumer> consumerProvider = mock(ObjectProvider.class);
        when(consumerProvider.getIfAvailable()).thenReturn(null);
        assertThatCode(() -> new ClaimConsumerStubDemo(leaseClient, consumerProvider).execute())
                .doesNotThrowAnyException();
    }
}
