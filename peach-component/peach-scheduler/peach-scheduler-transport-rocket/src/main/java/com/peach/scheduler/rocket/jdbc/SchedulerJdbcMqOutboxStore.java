package com.peach.scheduler.rocket.jdbc;

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

    /** 继承生产事务校验、租约及保留期，保留历史类型名称。 */
    public SchedulerJdbcMqOutboxStore(JdbcTemplate jdbcTemplate,
            com.peach.rocket.autoconfigure.PeachRocketProperties properties) {
        super(jdbcTemplate, properties);
    }
}
