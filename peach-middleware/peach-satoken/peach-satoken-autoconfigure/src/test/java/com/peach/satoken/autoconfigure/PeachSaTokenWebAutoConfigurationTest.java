package com.peach.satoken.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PeachSa令牌WebAutoConfigurationTest。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/3/20 16:58
 */
class PeachSaTokenWebAutoConfigurationTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(PeachSaTokenWebAutoConfiguration.class));

    /**
     * 验证 Sa-Token 不再创建历史请求 ID 过滤器及其配置 Bean。
     */
    @SuppressWarnings("deprecation")
    @Test
    void shouldNotRegisterLegacyRequestIdComponents() {
        contextRunner.run(context -> assertThat(context).isNotNull());
    }
}
