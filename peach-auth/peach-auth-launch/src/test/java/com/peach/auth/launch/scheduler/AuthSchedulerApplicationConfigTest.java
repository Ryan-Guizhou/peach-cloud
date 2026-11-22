package com.peach.auth.launch.scheduler;

import com.peach.scheduler.PeachSchedulerAutoConfiguration;
import com.peach.scheduler.PeachJobExecutorAutoConfiguration;
import com.peach.scheduler.transport.ExecutionLeaseClient;
import com.peach.scheduler.transport.ExecutionResultReporter;
import com.peach.virtualthread.autoconfigure.PeachVirtualThreadAutoConfiguration;
import com.peach.scheduler.core.JobRegistry;
import com.peach.scheduler.core.PeachJobExecutor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * 验证主应用配置可直接装配测试任务和命令消费者，不依赖额外 profile。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/21 23:00
 */
class AuthSchedulerApplicationConfigTest {
    @Test
    void applicationYamlShouldRegisterHandlerAndConsumer() throws Exception {
        var sources = new YamlPropertySourceLoader().load("main-application", new ClassPathResource("application.yml"));
        java.nio.file.Path root = java.nio.file.Path.of("").toAbsolutePath();
        while (!java.nio.file.Files.isDirectory(root.resolve("deploy/nacos/config"))) root = root.getParent();
        var loader = new YamlPropertySourceLoader();
        sources = new java.util.ArrayList<>(sources);
        sources.addAll(loader.load("shared-runtime", new org.springframework.core.io.FileSystemResource(
                root.resolve("deploy/nacos/config/peach-scheduler-executor.yml"))));
        sources.addAll(loader.load("service-runtime", new org.springframework.core.io.FileSystemResource(
                root.resolve("deploy/nacos/config/peach-auth.yml"))));
        final var runtimeSources = sources;
        new ApplicationContextRunner()
                .withPropertyValues("spring.application.name=peach-auth")
                .withConfiguration(AutoConfigurations.of(PeachVirtualThreadAutoConfiguration.class, PeachSchedulerAutoConfiguration.class, PeachJobExecutorAutoConfiguration.class))
                .withUserConfiguration(AuthSchedulerDemoJob.class, AuthSchedulerDemoConsumer.class)
                .withBean(ExecutionLeaseClient.class, () -> mock(ExecutionLeaseClient.class))
                .withBean(ExecutionResultReporter.class, () -> mock(ExecutionResultReporter.class))
                .withInitializer(context -> runtimeSources.forEach(source -> context.getEnvironment().getPropertySources().addLast(source)))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(PeachJobExecutor.class);
                    assertThat(context).hasSingleBean(AuthSchedulerDemoJob.class);
                    assertThat(context).hasSingleBean(AuthSchedulerDemoConsumer.class);
                    assertThat(context.getBean(JobRegistry.class).getRequired("authSchedulerDemo")).isNotNull();
                    var environment = context.getEnvironment();
                    assertThat(environment.getProperty("peach.scheduler.enabled", Boolean.class)).isTrue();
                    assertThat(environment.getProperty("peach.scheduler.executor.application-name")).isEqualTo("peach-auth");
                    assertThat(environment.getProperty("peach.rocket.outbox.enabled", Boolean.class)).isTrue();
                    assertThat(environment.getProperty("peach.openfeign.same-token-enabled", Boolean.class)).isTrue();
                    assertThat(environment.getProperty("rocketmq.producer.group")).isEqualTo("peach-auth-scheduler-producer");
                });
    }
}
