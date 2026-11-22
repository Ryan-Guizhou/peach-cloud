package com.peach.scheduled.external;

import com.peach.scheduler.config.PeachSchedulerProperties;
import com.peach.scheduler.core.JobRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证真实 Feign 客户端注册时 Handler 心跳初始化器不会被条件装配漏掉。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/21 13:15
 */
class SchedulerExternalAutoConfigurationTest {

    @Test
    void shouldRegisterHeartbeatWithFeignGeneratedClient() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(FeignAutoConfiguration.class,
                        HttpMessageConvertersAutoConfiguration.class, SchedulerExternalAutoConfiguration.class))
                .withBean(JobRegistry.class, JobRegistry::new)
                .withBean(PeachSchedulerProperties.class, PeachSchedulerProperties::new)
                .withPropertyValues(
                        "spring.cloud.openfeign.client.config.schedulerExecutionExternalClient.url=http://127.0.0.1:1",
                        "spring.cloud.openfeign.client.config.schedulerHandlerExternalClient.url=http://127.0.0.1:1")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(SchedulerHandlerRegistrationInitializer.class);
                });
    }
}
