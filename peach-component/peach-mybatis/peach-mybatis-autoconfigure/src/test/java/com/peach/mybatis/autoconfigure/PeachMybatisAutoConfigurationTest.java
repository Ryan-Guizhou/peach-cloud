package com.peach.mybatis.autoconfigure;

import com.peach.mybatis.interceptor.SqlCostInterceptor;
import org.apache.ibatis.mapping.DatabaseIdProvider;
import org.apache.ibatis.plugin.Interceptor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MyBatis 自动配置装配。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/9 16:40
 */
class PeachMybatisAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    PeachMybatisAutoConfiguration.class,
                    PeachMybatisPageHelperAutoConfiguration.class,
                    PeachMybatisMetricsAutoConfiguration.class));

    @Test
    void shouldRegisterDatabaseIdAndInterceptors() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(DatabaseIdProvider.class);
            assertThat(context).hasSingleBean(SqlCostInterceptor.class);
            assertThat(context).hasSingleBean(PeachMybatisSqlSessionFactoryPostProcessor.class);
            assertThat(context).getBeans(Interceptor.class).hasSize(2);
        });
    }

    @Test
    void shouldBackOffWhenDisabled() {
        runner.withPropertyValues("peach.mybatis.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(DatabaseIdProvider.class);
                    assertThat(context).doesNotHaveBean(SqlCostInterceptor.class);
                });
    }
}
