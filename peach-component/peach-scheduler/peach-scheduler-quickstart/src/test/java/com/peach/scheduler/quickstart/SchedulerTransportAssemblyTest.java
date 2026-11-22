package com.peach.scheduler.quickstart;

import com.peach.scheduler.PeachSchedulerAutoConfiguration;
import com.peach.scheduler.core.PeachJobExecutor;
import com.peach.scheduler.transport.ExecutionLeaseClient;
import com.peach.scheduler.transport.ExecutionResultReporter;
import com.peach.virtualthread.registry.VirtualExecutorRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.annotation.ImportCandidates;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * 验证传输层晚于注册表装配时仍能创建默认执行器。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/21 13:10
 */
class SchedulerTransportAssemblyTest {

    @Test
    void shouldCreateExecutorAfterTransportBeansBecomeAvailable() throws ClassNotFoundException {
        List<Class<?>> configurations = new ArrayList<>();
        for (String candidate : ImportCandidates.load(AutoConfiguration.class, getClass().getClassLoader())) {
            if (candidate.startsWith("com.peach.scheduler.") && !candidate.startsWith("com.peach.scheduler.rocket.")) {
                configurations.add(Class.forName(candidate));
            }
        }
        configurations.add(LateTransportConfiguration.class);
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(configurations.toArray(Class<?>[]::new)))
                .withBean(VirtualExecutorRegistry.class, () -> mock(VirtualExecutorRegistry.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(PeachJobExecutor.class);
                });
    }

    /**
     * 模拟在 Handler 注册表之后才可用的真实传输客户端。
     *
     * @Author Mr Shu
     * @Version 1.0.0
     * @CreateTime 2026/11/21 13:10
     */
    @AutoConfiguration(after = PeachSchedulerAutoConfiguration.class,
            beforeName = "com.peach.scheduler.PeachJobExecutorAutoConfiguration")
    static class LateTransportConfiguration {
        @Bean
        ExecutionLeaseClient leaseClient() {
            return (executionId, instanceId) -> true;
        }

        @Bean
        ExecutionResultReporter reporter() {
            return event -> { };
        }
    }
}
