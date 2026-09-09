package com.peach.virtualthread;

import com.peach.virtualthread.autoconfigure.PeachVirtualThreadAutoConfiguration;
import com.peach.virtualthread.autoconfigure.PeachVirtualThreadMetricsAutoConfiguration;
import com.peach.virtualthread.executor.VirtualThreadMetricsBinder;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class VirtualThreadMetricsAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    PeachVirtualThreadAutoConfiguration.class,
                    PeachVirtualThreadMetricsAutoConfiguration.class))
            .withUserConfiguration(MeterConfiguration.class);

    @Test
    void registersOnlyLowCardinalityGroupTaggedMeters() {
        contextRunner
                .withPropertyValues(
                        "peach.virtual-thread.groups.database.max-concurrency=3",
                        "peach.virtual-thread.groups.storage.max-concurrency=4"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    MeterRegistry registry = context.getBean(MeterRegistry.class);
                    VirtualThreadMetricsBinder binder = context.getBean(VirtualThreadMetricsBinder.class);
                    binder.bindTo(registry);
                    assertThat(registry.find("peach.virtual.thread.running").tag("group", "database").gauge())
                            .isNotNull();
                    assertThat(registry.find("peach.virtual.thread.running").tag("group", "storage").gauge())
                            .isNotNull();
                    registry.getMeters().forEach(meter ->
                            assertThat(meter.getId().getTags())
                                    .allMatch(tag -> tag.getKey().equals("group")));
                });
    }

    @Configuration(proxyBeanMethods = false)
    static class MeterConfiguration {
        @Bean
        MeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }
    }
}
