package com.peach.scheduler.quickstart;

import com.peach.scheduler.quickstart.config.DemoExecutionLeaseClient;
import com.peach.scheduler.quickstart.config.DemoExecutionResultReporter;
import com.peach.scheduler.quickstart.config.DemoSchedulerIntegrationConfiguration;
import com.peach.scheduler.quickstart.example.ClaimGateExample;
import com.peach.scheduler.quickstart.example.ExecutorOrchestrationExample;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证真实联调模式不会装配依赖内存桩的演示流程。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/21 13:00
 */
class SchedulerIntegrationModeTest {

    @Test
    void integrationModeShouldExcludeLocalStubsAndTheirExamples() {
        new ApplicationContextRunner()
                .withUserConfiguration(DemoSchedulerIntegrationConfiguration.class,
                        ClaimGateExample.class, ExecutorOrchestrationExample.class)
                .withPropertyValues("peach.scheduler.quickstart.local-mode=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(DemoExecutionLeaseClient.class);
                    assertThat(context).doesNotHaveBean(DemoExecutionResultReporter.class);
                    assertThat(context).doesNotHaveBean(ClaimGateExample.class);
                    assertThat(context).doesNotHaveBean(ExecutorOrchestrationExample.class);
                });
    }
}
