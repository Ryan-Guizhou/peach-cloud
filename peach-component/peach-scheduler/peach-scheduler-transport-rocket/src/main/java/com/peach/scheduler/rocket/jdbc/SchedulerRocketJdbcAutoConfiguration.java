package com.peach.scheduler.rocket.jdbc;

import org.springframework.stereotype.Indexed;

import com.peach.rocket.autoconfigure.PeachRocketAutoConfigure;
import com.peach.rocket.autoconfigure.PeachRocketOutboxAutoConfigure;
import com.peach.rocket.idempotent.MqIdempotentStore;
import com.peach.rocket.outbox.MqOutboxStore;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 调度模块相关说明。
 *
 * <p>调度模块相关说明。
 * 调度模块相关说明。
 * 调度模块相关说明。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
@AutoConfiguration
@AutoConfigureBefore({PeachRocketAutoConfigure.class, PeachRocketOutboxAutoConfigure.class})
@ConditionalOnClass(JdbcTemplate.class)
@ConditionalOnBean(JdbcTemplate.class)
@Indexed
public class SchedulerRocketJdbcAutoConfiguration {

    /**
     * 创建相关对象。
     */
    public SchedulerRocketJdbcAutoConfiguration() {
    }

    /**
     * 创建相关对象。
     *
     * @param jdbcTemplate 参数说明
     * @return 返回结果
     */
    @Bean
    @ConditionalOnMissingBean(MqOutboxStore.class)
    public MqOutboxStore schedulerJdbcMqOutboxStore(JdbcTemplate jdbcTemplate) {
        return new SchedulerJdbcMqOutboxStore(jdbcTemplate);
    }

    /**
     * 创建相关对象。
     *
     * @param jdbcTemplate 参数说明
     * @return 返回结果
     */
    @Bean
    @ConditionalOnMissingBean(MqIdempotentStore.class)
    public MqIdempotentStore schedulerJdbcMqIdempotentStore(JdbcTemplate jdbcTemplate) {
        return new SchedulerJdbcMqIdempotentStore(jdbcTemplate);
    }
}
