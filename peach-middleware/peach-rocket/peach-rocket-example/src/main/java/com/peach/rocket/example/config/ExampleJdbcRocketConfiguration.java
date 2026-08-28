package com.peach.rocket.example.config;

import org.springframework.stereotype.Indexed;
import com.peach.rocket.example.config.jdbc.ExampleJdbcMqIdempotentStore;
import com.peach.rocket.example.config.jdbc.ExampleJdbcMqOutboxStore;
import com.peach.rocket.idempotent.MqIdempotentStore;
import com.peach.rocket.outbox.MqOutboxStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * ExampleJdbcRocketMQ配置类。
 * <p>当示例应用中存在 {@link JdbcTemplate} 时，通过显式声明 Bean 覆盖 starter 默认的内存实现，
 * 用于演示如何在业务项目里接入 JDBC 幂等和 JDBC Outbox。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/26
 */
@Indexed
@Configuration
@ConditionalOnBean(JdbcTemplate.class)
public class ExampleJdbcRocketConfiguration {

    /**
     * 覆盖默认幂等存储为 JDBC 实现。
     *
     * @param jdbcTemplate JDBC 操作模板
     * @return JDBC 幂等存储
     */
    @Bean
    public MqIdempotentStore exampleJdbcMqIdempotentStore(JdbcTemplate jdbcTemplate) {
        return new ExampleJdbcMqIdempotentStore(jdbcTemplate);
    }

    /**
     * 覆盖默认 Outbox 存储为 JDBC 实现。
     *
     * @param jdbcTemplate JDBC 操作模板
     * @return JDBC Outbox 存储
     */
    @Bean
    public MqOutboxStore exampleJdbcMqOutboxStore(JdbcTemplate jdbcTemplate) {
        return new ExampleJdbcMqOutboxStore(jdbcTemplate);
    }
}
