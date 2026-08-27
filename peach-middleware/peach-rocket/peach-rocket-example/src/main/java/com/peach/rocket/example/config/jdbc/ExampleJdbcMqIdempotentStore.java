package com.peach.rocket.example.config.jdbc;

import java.time.ZoneId;

import com.peach.rocket.idempotent.MqIdempotentContext;
import com.peach.rocket.idempotent.MqIdempotentStore;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Example 模块中的 JDBC 幂等存储实现。
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/26
 */
public class ExampleJdbcMqIdempotentStore implements MqIdempotentStore {
    private static final String SQL_UPDATE_PREFIX = "UPDATE ";

    private static final String STATUS_PROCESSING = "PROCESSING";


    /**
     * 示例幂等表名。
     */
    public static final String TABLE_NAME = "mq_consume_record";

    private final JdbcTemplate jdbcTemplate;

    public ExampleJdbcMqIdempotentStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public boolean tryStart(MqIdempotentContext context) {
        LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
        try {
            jdbcTemplate.update("INSERT INTO " + TABLE_NAME
                            + " (idempotent_key, consumer_group, topic, tag, business_key, message_id, status, consume_count, created_at, updated_at)"
                            + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    context.getIdempotentKey(), context.getConsumerGroup(), context.getTopic(), context.getTag(),
                    context.getBusinessKey(), context.getMessageId(), STATUS_PROCESSING, 1, Timestamp.valueOf(now), Timestamp.valueOf(now));
            return true;
        } catch (DuplicateKeyException ex) {
            LocalDateTime timeoutBefore = now.minus(context.getExpire());
            int updated = jdbcTemplate.update(SQL_UPDATE_PREFIX + TABLE_NAME
                            + " SET status = ?, topic = ?, tag = ?, business_key = ?, message_id = ?, consume_count = consume_count + 1, updated_at = ?"
                            + " WHERE idempotent_key = ? AND consumer_group = ? AND (status = ? OR (status = ? AND updated_at < ?))",
                    STATUS_PROCESSING, context.getTopic(), context.getTag(), context.getBusinessKey(), context.getMessageId(),
                    Timestamp.valueOf(now), context.getIdempotentKey(), context.getConsumerGroup(), "FAILED", STATUS_PROCESSING,
                    Timestamp.valueOf(timeoutBefore));
            return updated == 1;
        }
    }

    @Override
    public void markSuccess(MqIdempotentContext context) {
        jdbcTemplate.update(SQL_UPDATE_PREFIX + TABLE_NAME + " SET status = ?, updated_at = ? WHERE idempotent_key = ? AND consumer_group = ?",
                "SUCCESS", Timestamp.valueOf(LocalDateTime.now(ZoneId.systemDefault())), context.getIdempotentKey(), context.getConsumerGroup());
    }

    @Override
    public void markFailed(MqIdempotentContext context) {
        jdbcTemplate.update(SQL_UPDATE_PREFIX + TABLE_NAME + " SET status = ?, updated_at = ? WHERE idempotent_key = ? AND consumer_group = ?",
                "FAILED", Timestamp.valueOf(LocalDateTime.now(ZoneId.systemDefault())), context.getIdempotentKey(), context.getConsumerGroup());
    }

    @Override
    public boolean isSuccess(MqIdempotentContext context) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM " + TABLE_NAME + " WHERE idempotent_key = ? AND consumer_group = ? AND status = ?",
                Integer.class, context.getIdempotentKey(), context.getConsumerGroup(), "SUCCESS");
        return count != null && count > 0;
    }
}
