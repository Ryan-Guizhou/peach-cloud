package com.peach.scheduler.rocket.jdbc;

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

    /** 继承统一租约配置，避免 Scheduler 适配层回退到默认处理期限。 */
    public SchedulerJdbcMqIdempotentStore(JdbcTemplate jdbcTemplate,
            com.peach.rocket.autoconfigure.PeachRocketProperties properties) {
        super(jdbcTemplate, properties);
    }
}
