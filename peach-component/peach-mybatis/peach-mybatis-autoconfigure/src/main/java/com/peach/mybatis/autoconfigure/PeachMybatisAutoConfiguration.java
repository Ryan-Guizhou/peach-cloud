package com.peach.mybatis.autoconfigure;

import com.peach.mybatis.config.PeachMybatisProperties;
import com.peach.mybatis.dialect.PeachDatabaseIdProvider;
import com.peach.mybatis.interceptor.SqlCostInterceptor;
import com.peach.mybatis.metrics.SqlMetricRecorder;
import org.apache.ibatis.mapping.DatabaseIdProvider;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;

/**
 * MyBatis 多库识别与 SQL 日志自动配置。
 * <p>官方自动配置会收集这里的 {@code DatabaseIdProvider} 和拦截器。
 * 启动类自己创建 {@code SqlSessionFactoryBean} 时，由后处理器把它们装回去。
 * 日志默认是带 {@code ?} 的语句加 Parameters；{@code peach.mybatis.log.executable=true} 时只输出可执行 SQL。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/9 16:40
 */
@AutoConfiguration
@ConditionalOnClass(SqlSessionFactoryBean.class)
@ConditionalOnProperty(prefix = "peach.mybatis", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(PeachMybatisProperties.class)
public class PeachMybatisAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(DatabaseIdProvider.class)
    public DatabaseIdProvider databaseIdProvider(PeachMybatisProperties properties) {
        return new PeachDatabaseIdProvider(properties.resolvedDatabaseIds());
    }

    @Bean
    @Order(100)
    @ConditionalOnMissingBean(SqlCostInterceptor.class)
    public SqlCostInterceptor sqlCostInterceptor(PeachMybatisProperties properties,
                                                 ObjectProvider<SqlMetricRecorder> metricRecorder) {
        return new SqlCostInterceptor(properties, metricRecorder.getIfAvailable());
    }

    @Bean
    @ConditionalOnMissingBean(PeachMybatisSqlSessionFactoryPostProcessor.class)
    public static PeachMybatisSqlSessionFactoryPostProcessor peachMybatisSqlSessionFactoryPostProcessor(
            PeachMybatisProperties properties) {
        return new PeachMybatisSqlSessionFactoryPostProcessor(properties);
    }
}
