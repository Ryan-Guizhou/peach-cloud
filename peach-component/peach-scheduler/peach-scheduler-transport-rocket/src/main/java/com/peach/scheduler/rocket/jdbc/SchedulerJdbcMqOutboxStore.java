package com.peach.scheduler.rocket.jdbc;

import java.time.ZoneId;

import org.springframework.stereotype.Indexed;

import com.peach.rocket.outbox.MqOutboxEvent;
import com.peach.rocket.outbox.MqOutboxStatus;
import com.peach.rocket.outbox.MqOutboxStore;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

/**
 * 调度JdbcMQ发件箱存储。
 * <p>调度模块说明。
 * 调度模块说明。
 * 调度模块说明。
 * 调度模块说明。
 * 调度模块说明。</p>
 * <p>调度模块说明。
 * 调度模块说明。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
@Indexed
public class SchedulerJdbcMqOutboxStore implements MqOutboxStore {

    private static final int MAX_RETRY_COUNT = 20;
    private static final int MAX_BATCH_SIZE = 500;
    private static final int CLAIM_TIMEOUT_MINUTES = 5;

    private static final String INSERT_SQL =
            "INSERT INTO MQ_OUTBOX_EVENT(MESSAGE_ID,TOPIC,TAG,BUSINESS_KEY,PAYLOAD,SEND_MODE,STATUS,"
                    + "RETRY_COUNT,NEXT_RETRY_TIME,CREATED_AT,UPDATED_AT) VALUES(?,?,?,?,?,?,?,?,?,?,?)";

    private final JdbcTemplate jdbcTemplate;
    private final String claimantId;

    /**
     * 创建实例。
     *
     * @param jdbcTemplate jdbc Template。
     */
    public SchedulerJdbcMqOutboxStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.claimantId = "scheduler-outbox-" + UUID.randomUUID().toString();
    }

    /**
     * 接口实现。
     */
    @Override
    public void save(MqOutboxEvent event) {
        LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
        jdbcTemplate.update(INSERT_SQL,
                event.messageId(),
                event.topic(),
                event.tag(),
                event.businessKey(),
                Base64.getEncoder().encodeToString(event.body()),
                "NORMAL",
                MqOutboxStatus.INIT.name(),
                0,
                Timestamp.valueOf(now),
                Timestamp.valueOf(now),
                Timestamp.valueOf(now));
    }

    /**
     * 接口实现。
     */
    @Override
    public List<MqOutboxEvent> findPending(int batchSize) {
        int boundedBatch = Math.clamp(batchSize, 1, MAX_BATCH_SIZE);
        recoverStaleClaims();
        jdbcTemplate.update(
                "UPDATE MQ_OUTBOX_EVENT SET STATUS=?,CLAIMED_BY=?,CLAIMED_AT=NOW(3),UPDATED_AT=NOW(3) "
                        + "WHERE STATUS IN (?,?) AND NEXT_RETRY_TIME<=NOW(3) "
                        + "ORDER BY UPDATED_AT ASC LIMIT ?",
                MqOutboxStatus.SENDING.name(),
                claimantId,
                MqOutboxStatus.INIT.name(),
                MqOutboxStatus.RETRY.name(),
                boundedBatch);
        return jdbcTemplate.query(
                "SELECT MESSAGE_ID,TOPIC,TAG,BUSINESS_KEY,PAYLOAD,STATUS,RETRY_COUNT,CREATED_AT,UPDATED_AT "
                        + "FROM MQ_OUTBOX_EVENT WHERE STATUS=? AND CLAIMED_BY=? ORDER BY CLAIMED_AT ASC",
                new OutboxRowMapper(),
                MqOutboxStatus.SENDING.name(),
                claimantId);
    }

    /**
     * 接口实现。
     */
    @Override
    public void markSent(String messageId) {
        jdbcTemplate.update(
                "UPDATE MQ_OUTBOX_EVENT SET STATUS=?,SENT_AT=NOW(3),CLAIMED_BY=NULL,CLAIMED_AT=NULL,UPDATED_AT=NOW(3) "
                        + "WHERE MESSAGE_ID=? AND STATUS=?",
                MqOutboxStatus.SENT.name(),
                messageId,
                MqOutboxStatus.SENDING.name());
    }

    /**
     * 接口实现。
     */
    @Override
    public void markFailed(String messageId) {
        jdbcTemplate.update(
                "UPDATE MQ_OUTBOX_EVENT SET "
                        + "STATUS=CASE WHEN RETRY_COUNT+1>=? THEN ? ELSE ? END,"
                        + "RETRY_COUNT=RETRY_COUNT+1,"
                        + "NEXT_RETRY_TIME=DATE_ADD(NOW(3), INTERVAL LEAST(300, POW(2,RETRY_COUNT)) SECOND),"
                        + "CLAIMED_BY=NULL,CLAIMED_AT=NULL,UPDATED_AT=NOW(3) "
                        + "WHERE MESSAGE_ID=? AND STATUS=?",
                MAX_RETRY_COUNT,
                MqOutboxStatus.FAILED.name(),
                MqOutboxStatus.RETRY.name(),
                messageId,
                MqOutboxStatus.SENDING.name());
    }

    /**
     * 接口实现。
     */
    @Override
    public boolean replay(String messageId) {
        return jdbcTemplate.update(
                "UPDATE MQ_OUTBOX_EVENT SET STATUS=?,RETRY_COUNT=0,NEXT_RETRY_TIME=NOW(3),"
                        + "CLAIMED_BY=NULL,CLAIMED_AT=NULL,UPDATED_AT=NOW(3) WHERE MESSAGE_ID=? AND STATUS=?",
                MqOutboxStatus.RETRY.name(),
                messageId,
                MqOutboxStatus.FAILED.name()) == 1;
    }

    /**
     * 获取相关数据。
     */
    private void recoverStaleClaims() {
        jdbcTemplate.update(
                "UPDATE MQ_OUTBOX_EVENT SET STATUS=?,CLAIMED_BY=NULL,CLAIMED_AT=NULL,NEXT_RETRY_TIME=NOW(3),"
                        + "UPDATED_AT=NOW(3) WHERE STATUS=? AND CLAIMED_AT<DATE_SUB(NOW(3), INTERVAL ? MINUTE)",
                MqOutboxStatus.RETRY.name(),
                MqOutboxStatus.SENDING.name(),
                CLAIM_TIMEOUT_MINUTES);
    }

    /**
     * 发件箱RowMapper。
     *
     * @Author Mr Shu
     * @Version 1.0.0
     * @CreateTime 2026/3/20 16:58
     */
    private static final class OutboxRowMapper implements RowMapper<MqOutboxEvent> {

        /**
         * 接口实现。
         */
        @Override
        public MqOutboxEvent mapRow(ResultSet resultSet, int rowNum) throws SQLException {
            Timestamp createdAt = resultSet.getTimestamp("CREATED_AT");
            Timestamp updatedAt = resultSet.getTimestamp("UPDATED_AT");
            return new MqOutboxEvent(
                    resultSet.getString("MESSAGE_ID"),
                    Base64.getDecoder().decode(resultSet.getString("PAYLOAD")),
                    resultSet.getString("TOPIC"),
                    resultSet.getString("TAG"),
                    resultSet.getString("BUSINESS_KEY"),
                    null,
                    MqOutboxStatus.valueOf(resultSet.getString("STATUS")),
                    resultSet.getInt("RETRY_COUNT"),
                    createdAt == null ? null : createdAt.toLocalDateTime(),
                    updatedAt == null ? null : updatedAt.toLocalDateTime());
        }
    }
}
