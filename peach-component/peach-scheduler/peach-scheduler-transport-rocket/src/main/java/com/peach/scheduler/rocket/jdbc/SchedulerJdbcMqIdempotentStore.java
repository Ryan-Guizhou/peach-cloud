package com.peach.scheduler.rocket.jdbc;

import com.peach.rocket.autoconfigure.PeachRocketProperties;
import com.peach.rocket.jdbc.JdbcMqIdempotentStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Indexed;

/**
 * 兼容 Scheduler 历史类型名称的通用 JDBC 幂等存储适配器。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/12
 */
@Indexed
public class SchedulerJdbcMqIdempotentStore extends JdbcMqIdempotentStore {

    /**
     * @param jdbcTemplate JDBC 操作模板
     */
    public SchedulerJdbcMqIdempotentStore(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }

    /**
     * 使用统一的幂等处理租约配置构造调度存储，避免继承父类默认期限。
     *
     * @param jdbcTemplate JDBC 操作模板
     * @param properties RocketMQ 统一配置
     */
    public SchedulerJdbcMqIdempotentStore(JdbcTemplate jdbcTemplate,
            PeachRocketProperties properties) {
        super(jdbcTemplate, properties);
    }
}
