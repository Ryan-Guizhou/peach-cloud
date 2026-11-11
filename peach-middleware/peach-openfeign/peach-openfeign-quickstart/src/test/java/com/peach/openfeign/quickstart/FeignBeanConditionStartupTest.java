package com.peach.openfeign.quickstart;

import com.peach.openfeign.autoconfigure.PeachOpenfeignWebAutoConfiguration;
import com.peach.openfeign.support.PeachOpenfeignNamedClientMerger;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.cloud.openfeign.FeignClientProperties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 保护 Feign 配置装配边界，避免缺少 FeignClientProperties 时导致业务服务启动失败。
 */
class FeignBeanConditionStartupTest {

    private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(PeachOpenfeignWebAutoConfiguration.class));

    @Test
    void shouldSkipNamedClientMergerWithoutFeignPropertiesBean() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(PeachOpenfeignNamedClientMerger.class);
        });
    }

    @Test
    void shouldCreateMergerWhenFeignPropertiesBeanExists() {
        runner.withBean(FeignClientProperties.class, FeignClientProperties::new)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(PeachOpenfeignNamedClientMerger.class);
                });
    }
}
