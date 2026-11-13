package com.peach.scheduler.rocket.jdbc;

import com.peach.rocket.autoconfigure.PeachRocketProperties;
import com.peach.rocket.jdbc.JdbcMqOutboxStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Indexed;

/**
 * 兼容 Scheduler 历史类型名称的通用 JDBC Outbox 适配器。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/12
 */
@Indexed
public class SchedulerJdbcMqOutboxStore extends JdbcMqOutboxStore {

    /**
     * @param jdbcTemplate JDBC 操作模板
     */
    public SchedulerJdbcMqOutboxStore(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }

    /**
     * 使用统一生产校验、领取租约和保留期配置构造 Outbox 存储。
     *
     * @param jdbcTemplate JDBC 操作模板
     * @param properties RocketMQ 统一配置
     */
    public SchedulerJdbcMqOutboxStore(JdbcTemplate jdbcTemplate,
            PeachRocketProperties properties) {
        super(jdbcTemplate, properties);
    }
}
