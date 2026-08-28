package com.peach.observability.autoconfigure;

import com.peach.observability.core.RequestIdGenerator;
import com.peach.observability.core.RequestIdResolver;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PeachObservabilityAutoConfigurationTest。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/3/20 16:58
 */

class PeachObservabilityAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(PeachObservabilityAutoConfiguration.class));

    @Test
    void shouldCreateDefaultRequestIdInfrastructure() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(RequestIdGenerator.class);
            assertThat(context).hasSingleBean(RequestIdResolver.class);
        });
    }

    @Test
    void shouldBackOffWhenDisabled() {
        contextRunner.withPropertyValues("peach.observability.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(RequestIdResolver.class));
    }

    @Test
    void shouldAllowCustomRequestIdGenerator() {
        contextRunner.withBean(RequestIdGenerator.class, () -> () -> "custom-request-id")
                .run(context -> assertThat(context.getBean(RequestIdGenerator.class).generate())
                        .isEqualTo("custom-request-id"));
    }
}
