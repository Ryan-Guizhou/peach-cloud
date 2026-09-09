package com.peach.virtualthread;

import com.peach.virtualthread.annotation.VirtualGroup;
import com.peach.virtualthread.api.VirtualExecutorService;
import com.peach.virtualthread.autoconfigure.PeachVirtualThreadAutoConfiguration;
import com.peach.virtualthread.exception.VirtualTaskExceptionHandler;
import com.peach.virtualthread.registry.VirtualExecutorRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class PeachVirtualThreadAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(PeachVirtualThreadAutoConfiguration.class));

    @Test
    void createsOneExecutorBeanPerConfiguredGroup() {
        contextRunner
                .withPropertyValues(
                        "peach.virtual-thread.groups.database.max-concurrency=12",
                        "peach.virtual-thread.groups.database.max-pending=24",
                        "peach.virtual-thread.groups.storage.max-concurrency=4",
                        "peach.virtual-thread.groups.storage.max-pending=8",
                        "peach.virtual-thread.groups.storage.backpressure=REJECT"
                )
                .run(context -> {
                    assertThat(context).hasBean("databaseVirtualExecutor");
                    assertThat(context).hasBean("storageVirtualExecutor");
                    assertThat(context).doesNotHaveBean("defaultVirtualExecutor");
                    assertThat(context.getBean("databaseVirtualExecutor")).isInstanceOf(VirtualExecutorService.class);
                    assertThat(context.getBean(VirtualExecutorRegistry.class).groupNames())
                            .containsExactlyInAnyOrder("database", "storage");
                });
    }

    @Test
    void injectsExecutorByVirtualGroupQualifier() {
        contextRunner
                .withUserConfiguration(QualifierConfiguration.class)
                .withPropertyValues("peach.virtual-thread.groups.database.max-concurrency=3")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    GroupClient client = context.getBean(GroupClient.class);
                    assertThat(client.executor().groupName()).isEqualTo("database");
                });
    }

    @Test
    void canDisableStarterCompletely() {
        contextRunner
                .withPropertyValues(
                        "peach.virtual-thread.enabled=false",
                        "peach.virtual-thread.groups.database.max-concurrency=3"
                )
                .run(context -> {
                    assertThat(context).doesNotHaveBean("databaseVirtualExecutor");
                    assertThat(context).doesNotHaveBean(VirtualExecutorRegistry.class);
                });
    }

    @Test
    void failsFastForInvalidBlockTimeout() {
        contextRunner
                .withPropertyValues(
                        "peach.virtual-thread.groups.remote.backpressure=BLOCK",
                        "peach.virtual-thread.groups.remote.acquire-timeout=0ms"
                )
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void allowsBusinessToOverrideExceptionHandler() {
        contextRunner
                .withUserConfiguration(CustomHandlerConfiguration.class)
                .withPropertyValues("peach.virtual-thread.groups.database.max-concurrency=3")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(VirtualTaskExceptionHandler.class);
                    assertThat(context.getBean(VirtualTaskExceptionHandler.class))
                            .isSameAs(context.getBean("customVirtualTaskExceptionHandler"));
                });
    }

    @Configuration(proxyBeanMethods = false)
    static class QualifierConfiguration {
        @Bean
        GroupClient groupClient(@VirtualGroup("database") VirtualExecutorService executor) {
            return new GroupClient(executor);
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomHandlerConfiguration {
        @Bean
        VirtualTaskExceptionHandler customVirtualTaskExceptionHandler() {
            return (context, throwable) -> { };
        }
    }

    record GroupClient(VirtualExecutorService executor) {
    }
}
