package com.peach.scheduler.rocket.jdbc;

import org.springframework.stereotype.Indexed;

import com.peach.rocket.autoconfigure.PeachRocketAutoConfigure;
import com.peach.rocket.autoconfigure.PeachRocketOutboxAutoConfigure;
import com.peach.rocket.idempotent.MqIdempotentStore;
import com.peach.rocket.outbox.MqOutboxStore;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 调度RocketMQJdbc自动配置。
 * <p>在 Spring Boot 完成 {@link JdbcTemplate} 自动配置后，为调度消息提供持久化的
 * Outbox 与消费幂等存储。该配置必须先于 Peach RocketMQ 的通用存储配置执行，
 * 避免在数据库可用时回退到仅适合开发调试的内存实现。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
@AutoConfiguration
@AutoConfigureAfter(JdbcTemplateAutoConfiguration.class)
@AutoConfigureBefore({PeachRocketAutoConfigure.class, PeachRocketOutboxAutoConfigure.class})
@ConditionalOnClass(JdbcTemplate.class)
@ConditionalOnBean(JdbcTemplate.class)
@Indexed
public class SchedulerRocketJdbcAutoConfiguration {

    /**
     * 创建调度 RocketMQ JDBC 存储自动配置。
     */
    public SchedulerRocketJdbcAutoConfiguration() {
        // Intentionally empty.
    }

    /**
     * 创建基于 JDBC 的 Outbox 持久化存储。
     *
     * @param jdbcTemplate JDBC 操作模板
     * @return 调度消息 Outbox 持久化存储
     */
    @Bean
    @ConditionalOnMissingBean(MqOutboxStore.class)
    public MqOutboxStore schedulerJdbcMqOutboxStore(JdbcTemplate jdbcTemplate) {
        return new SchedulerJdbcMqOutboxStore(jdbcTemplate);
    }

    /**
     * 创建基于 JDBC 的消费幂等存储。
     *
     * @param jdbcTemplate JDBC 操作模板
     * @return 调度消息消费幂等存储
     */
    @Bean
    @ConditionalOnMissingBean(MqIdempotentStore.class)
    public MqIdempotentStore schedulerJdbcMqIdempotentStore(JdbcTemplate jdbcTemplate) {
        return new SchedulerJdbcMqIdempotentStore(jdbcTemplate);
    }
}
