package com.peach.rocket.example.config.jdbc;

import java.time.ZoneId;

import com.peach.rocket.outbox.MqOutboxEvent;
import com.peach.rocket.outbox.MqOutboxStatus;
import com.peach.rocket.outbox.MqOutboxStore;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

/**
 * ExampleJdbcMQ发件箱存储。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/26
 */
public class ExampleJdbcMqOutboxStore implements MqOutboxStore {
    private static final String SQL_UPDATE_PREFIX = "UPDATE ";


    /**
     * 示例 Outbox 表名。
     */
    public static final String TABLE_NAME = "mq_outbox_event";

    private final JdbcTemplate jdbcTemplate;

    public ExampleJdbcMqOutboxStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void save(MqOutboxEvent event) {
        LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
        jdbcTemplate.update("INSERT INTO " + TABLE_NAME
                        + " (message_id, topic, tag, business_key, payload, send_mode, status, retry_count, next_retry_time, created_at, updated_at)"
                        + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                event.messageId(), event.topic(), event.tag(), event.businessKey(),
                Base64.getEncoder().encodeToString(event.body()), "NORMAL", MqOutboxStatus.INIT.name(), 0,
                Timestamp.valueOf(now), Timestamp.valueOf(now), Timestamp.valueOf(now));
    }

    @Override
    public List<MqOutboxEvent> findPending(int batchSize) {
        return jdbcTemplate.query("SELECT message_id, topic, tag, business_key, payload, status, retry_count, created_at, updated_at FROM "
                        + TABLE_NAME + " WHERE status IN (?, ?) ORDER BY updated_at ASC LIMIT ?",
                new OutboxRowMapper(), MqOutboxStatus.INIT.name(), MqOutboxStatus.RETRY.name(), batchSize);
    }

    @Override
    public void markSent(String messageId) {
        jdbcTemplate.update(SQL_UPDATE_PREFIX + TABLE_NAME + " SET status = ?, sent_at = ?, updated_at = ? WHERE message_id = ?",
                MqOutboxStatus.SENT.name(), Timestamp.valueOf(LocalDateTime.now(ZoneId.systemDefault())), Timestamp.valueOf(LocalDateTime.now(ZoneId.systemDefault())), messageId);
    }

    @Override
    public void markFailed(String messageId) {
        jdbcTemplate.update(SQL_UPDATE_PREFIX + TABLE_NAME + " SET status = ?, retry_count = retry_count + 1, updated_at = ? WHERE message_id = ?",
                MqOutboxStatus.RETRY.name(), Timestamp.valueOf(LocalDateTime.now(ZoneId.systemDefault())), messageId);
    }

    @Override
    public boolean replay(String messageId) {
        int updated = jdbcTemplate.update(SQL_UPDATE_PREFIX + TABLE_NAME + " SET status = ?, retry_count = 0, updated_at = ? WHERE message_id = ? AND status = ?",
                MqOutboxStatus.RETRY.name(), Timestamp.valueOf(LocalDateTime.now(ZoneId.systemDefault())), messageId, MqOutboxStatus.FAILED.name());
        return updated == 1;
    }

    /**
     * 发件箱RowMapper。
     *
     * @Author Mr Shu
     * @Version 1.0.0
     * @CreateTime 2026/3/20 16:58
     */

    private static final class OutboxRowMapper implements RowMapper<MqOutboxEvent> {
        @Override
        public MqOutboxEvent mapRow(ResultSet rs, int rowNum) throws SQLException {
            Timestamp createdAt = rs.getTimestamp("created_at");
            Timestamp updatedAt = rs.getTimestamp("updated_at");
            return new MqOutboxEvent(
                    rs.getString("message_id"),
                    Base64.getDecoder().decode(rs.getString("payload")),
                    rs.getString("topic"),
                    rs.getString("tag"),
                    rs.getString("business_key"),
                    null,
                    MqOutboxStatus.valueOf(rs.getString("status")),
                    rs.getInt("retry_count"),
                    createdAt == null ? null : createdAt.toLocalDateTime(),
                    updatedAt == null ? null : updatedAt.toLocalDateTime());
        }
    }
}
