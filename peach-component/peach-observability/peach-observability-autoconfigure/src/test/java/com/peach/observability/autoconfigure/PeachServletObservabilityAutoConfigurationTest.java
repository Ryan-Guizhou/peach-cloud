package com.peach.observability.autoconfigure;

import com.peach.observability.web.RequestIdServletFilter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Servlet 请求 ID 自动装配开关测试。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/11
 */
class PeachServletObservabilityAutoConfigurationTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    PeachObservabilityAutoConfiguration.class,
                    PeachServletObservabilityAutoConfiguration.class));

    @Test
    void shouldNotRegisterServletFilterWhenRequestIdDisabled() {
        contextRunner.withPropertyValues("peach.observability.request-id.enabled=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(RequestIdServletFilter.class);
                    assertThat(context).doesNotHaveBean("peachRequestIdServletFilterRegistration");
                });
    }

    @Test
    void shouldRegisterServletFilterWhenEnabled() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(RequestIdServletFilter.class);
            assertThat(context).hasBean("peachRequestIdServletFilterRegistration");
        });
    }
}
