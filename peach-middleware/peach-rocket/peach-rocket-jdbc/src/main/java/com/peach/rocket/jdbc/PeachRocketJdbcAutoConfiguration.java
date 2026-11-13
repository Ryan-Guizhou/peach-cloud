package com.peach.rocket.jdbc;

import com.peach.rocket.autoconfigure.PeachRocketAutoConfigure;
import com.peach.rocket.autoconfigure.PeachRocketProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.peach.rocket.autoconfigure.PeachRocketOutboxAutoConfigure;
import com.peach.rocket.idempotent.MqIdempotentStore;
import com.peach.rocket.outbox.MqOutboxStore;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 通用 RocketMQ JDBC 存储自动装配，默认不启用，避免误使用业务数据源。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/12
 */
@EnableConfigurationProperties(PeachRocketProperties.class)
@AutoConfiguration
@AutoConfigureAfter(JdbcTemplateAutoConfiguration.class)
@AutoConfigureBefore({PeachRocketAutoConfigure.class, PeachRocketOutboxAutoConfigure.class})
@ConditionalOnClass(JdbcTemplate.class)
@ConditionalOnBean(JdbcTemplate.class)
@ConditionalOnProperty(prefix = "peach.rocket.jdbc", name = "enabled", havingValue = "true")
public class PeachRocketJdbcAutoConfiguration {

    /**
     * @param jdbcTemplate 数据库操作模板
     * @return 持久化 Outbox
     */
    @Bean
    @ConditionalOnMissingBean(MqOutboxStore.class)
    public MqOutboxStore jdbcMqOutboxStore(JdbcTemplate jdbcTemplate, PeachRocketProperties properties) {
        return new JdbcMqOutboxStore(jdbcTemplate, properties);
    }

    /**
     * @param jdbcTemplate 数据库操作模板
     * @return 持久化幂等存储
     */
    @Bean
    @ConditionalOnMissingBean(MqIdempotentStore.class)
    public MqIdempotentStore jdbcMqIdempotentStore(JdbcTemplate jdbcTemplate, PeachRocketProperties properties) {
        return new JdbcMqIdempotentStore(jdbcTemplate, properties);
    }
}
