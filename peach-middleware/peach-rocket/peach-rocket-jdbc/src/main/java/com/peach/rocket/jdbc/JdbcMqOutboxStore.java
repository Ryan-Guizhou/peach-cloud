package com.peach.rocket.jdbc;

import java.time.ZoneId;
import com.peach.rocket.outbox.MqOutboxEvent;
import com.peach.rocket.core.MqDelay;
import com.peach.rocket.core.MqDelayLevels;
import com.peach.rocket.core.MqSendOptions;
import com.peach.rocket.exception.MqException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.util.StringUtils;
import com.peach.rocket.outbox.MqOutboxClaim;
import com.peach.rocket.outbox.MqOutboxStatus;
import com.peach.rocket.outbox.MqOutboxStore;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.Objects;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

/**
 * 通用 RocketMQ JDBC outbox 存储。
 *
 * <p>通过 MQ_OUTBOX_EVENT 保存待发送消息，支持批量认领、成功标记、失败重试和失败重放。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
public class JdbcMqOutboxStore implements MqOutboxStore {

    private static final int MAX_RETRY_COUNT = 20;
    private static final int MAX_BATCH_SIZE = 500;
    private static final int CLAIM_TIMEOUT_MINUTES = 5;

    private static final String INSERT_SQL =
            "INSERT INTO MQ_OUTBOX_EVENT(MESSAGE_ID,TOPIC,TAG,BUSINESS_KEY,PAYLOAD,SEND_MODE,"
                    + "SHARDING_KEY,DELAY_LEVEL,HEADERS,SEND_TIMEOUT_MS,STATUS,"
                    + "RETRY_COUNT,NEXT_RETRY_TIME,CREATED_AT,UPDATED_AT)"
                    + " VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, String>> HEADER_TYPE = new TypeReference<>() { };

    private final JdbcTemplate jdbcTemplate;

    /**
     * 创建 JDBC outbox 存储。
     *
     * @param jdbcTemplate JDBC 操作模板。
     */
    public JdbcMqOutboxStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate");
    }

    /**
     * 保存待发送消息。
     */
    @Override
    public void save(MqOutboxEvent event) {
        MqSendOptions options = event.options();
        String shardingKey = options == null ? null : options.getShardingKey();
        Integer delayLevel = options == null ? null : MqDelayLevels.resolve(options.getDelay());
        if (StringUtils.hasText(shardingKey) && delayLevel != null) {
            throw new MqException("Outbox cannot combine orderly and delayed sending");
        }
        String sendMode = StringUtils.hasText(shardingKey) ? "ORDERLY"
                : delayLevel != null ? "DELAY" : "NORMAL";
        Long timeout = options != null && options.getTimeoutMillis() > 0
                ? options.getTimeoutMillis() : null;
        String headers = null;
        if (options != null && options.getHeaders() != null && !options.getHeaders().isEmpty()) {
            try {
                headers = MAPPER.writeValueAsString(options.getHeaders());
            } catch (JsonProcessingException ex) {
                throw new MqException("Failed to serialize Outbox message headers", ex);
            }
        }
        LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
        jdbcTemplate.update(INSERT_SQL,
                event.messageId(),
                event.topic(),
                event.tag(),
                event.businessKey(),
                Base64.getEncoder().encodeToString(event.body()),
                sendMode,
                shardingKey,
                delayLevel,
                headers,
                timeout,
                MqOutboxStatus.INIT.name(),
                0,
                Timestamp.valueOf(now),
                Timestamp.valueOf(now),
                Timestamp.valueOf(now));
    }

    /**
     * 查询并认领待发送消息。
     */
    @Override
    public List<MqOutboxEvent> findPending(int batchSize) {
        return claimPending(batchSize).stream().map(MqOutboxClaim::event).toList();
    }

    /**
     * 使用每批唯一认领标识领取并读取本批消息，避免重复读取同进程尚未完成的旧批次。
     *
     * @param batchSize 最大批量大小
     * @return 本批次消息与认领令牌
     */
    @Override
    public List<MqOutboxClaim> claimPending(int batchSize) {
        int boundedBatch = Math.clamp(batchSize, 1, MAX_BATCH_SIZE);
        recoverStaleClaims();
        String batchClaimToken = "scheduler-outbox-" + UUID.randomUUID();
        jdbcTemplate.update(
                "UPDATE MQ_OUTBOX_EVENT SET STATUS=?,CLAIMED_BY=?,CLAIMED_AT=NOW(3),UPDATED_AT=NOW(3) "
                        + "WHERE STATUS IN (?,?) AND NEXT_RETRY_TIME<=NOW(3) "
                        + "ORDER BY UPDATED_AT ASC LIMIT ?",
                MqOutboxStatus.SENDING.name(),
                batchClaimToken,
                MqOutboxStatus.INIT.name(),
                MqOutboxStatus.RETRY.name(),
                boundedBatch);
        List<MqOutboxEvent> events = jdbcTemplate.query(
                "SELECT MESSAGE_ID,TOPIC,TAG,BUSINESS_KEY,PAYLOAD,SEND_MODE,SHARDING_KEY,"
                        + "DELAY_LEVEL,HEADERS,SEND_TIMEOUT_MS,STATUS,RETRY_COUNT,CREATED_AT,UPDATED_AT "
                        + "FROM MQ_OUTBOX_EVENT WHERE STATUS=? AND CLAIMED_BY=? "
                        + "ORDER BY CLAIMED_AT ASC LIMIT ?",
                new OutboxRowMapper(),
                MqOutboxStatus.SENDING.name(),
                batchClaimToken,
                boundedBatch);
        return events.stream().map(event -> new MqOutboxClaim(event, batchClaimToken)).toList();
    }

    /**
     * 只允许实际领取本消息的执行者完成状态变更，拒绝旧租约持有者覆盖新认领。
     *
     * @param claim 当前领取快照
     */
    @Override
    public void markSent(MqOutboxClaim claim) {
        String token = requireClaimToken(claim);
        int updated = jdbcTemplate.update(
                "UPDATE MQ_OUTBOX_EVENT SET STATUS=?,SENT_AT=NOW(3),CLAIMED_BY=NULL,"
                        + "CLAIMED_AT=NULL,UPDATED_AT=NOW(3) "
                        + "WHERE MESSAGE_ID=? AND STATUS=? AND CLAIMED_BY=?",
                MqOutboxStatus.SENT.name(), claim.event().messageId(),
                MqOutboxStatus.SENDING.name(), token);
        if (updated != 1) {
            throw new IllegalStateException("Outbox claim is stale when marking sent");
        }
    }

    /**
     * 当前认领失败时，只有令牌仍有效才更新重试状态。
     */
    @Override
    public void markFailed(MqOutboxClaim claim) {
        String token = requireClaimToken(claim);
        int updated = jdbcTemplate.update(
                "UPDATE MQ_OUTBOX_EVENT SET "
                        + "STATUS=CASE WHEN RETRY_COUNT+1>=? THEN ? ELSE ? END,"
                        + "RETRY_COUNT=RETRY_COUNT+1,"
                        + "NEXT_RETRY_TIME=DATE_ADD(NOW(3), INTERVAL LEAST(300, POW(2,RETRY_COUNT)) SECOND),"
                        + "CLAIMED_BY=NULL,CLAIMED_AT=NULL,UPDATED_AT=NOW(3) "
                        + "WHERE MESSAGE_ID=? AND STATUS=? AND CLAIMED_BY=?",
                MAX_RETRY_COUNT,
                MqOutboxStatus.FAILED.name(),
                MqOutboxStatus.RETRY.name(),
                claim.event().messageId(), MqOutboxStatus.SENDING.name(), token);
        if (updated != 1) {
            throw new IllegalStateException("Outbox claim is stale when marking failed");
        }
    }

    private static String requireClaimToken(MqOutboxClaim claim) {
        Objects.requireNonNull(claim, "claim");
        String token = claim.claimToken();
        if (!StringUtils.hasText(token)) {
            throw new IllegalArgumentException("JDBC Outbox requires a nonblank claim token");
        }
        return token;
    }

    /**
     * 标记消息已发送。
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
     * 标记消息发送失败并计算下一次重试时间。
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
     * 将最终失败消息重新放回重试队列。
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
     * 恢复超时未完成发送的认领记录。
     */
    private void recoverStaleClaims() {
        jdbcTemplate.update(
                "UPDATE MQ_OUTBOX_EVENT SET STATUS=?,CLAIMED_BY=NULL,CLAIMED_AT=NULL,NEXT_RETRY_TIME=NOW(3),"
                        + "UPDATED_AT=NOW(3) WHERE STATUS=? AND CLAIMED_AT<DATE_SUB(NOW(3), INTERVAL ? MINUTE) "
                        + "ORDER BY CLAIMED_AT ASC LIMIT ?",
                MqOutboxStatus.RETRY.name(),
                MqOutboxStatus.SENDING.name(),
                CLAIM_TIMEOUT_MINUTES,
                MAX_BATCH_SIZE);
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
         * 将 outbox 表记录映射为消息事件。
         */
        @Override
        public MqOutboxEvent mapRow(ResultSet resultSet, int rowNum) throws SQLException {
            Timestamp createdAt = resultSet.getTimestamp("CREATED_AT");
            Timestamp updatedAt = resultSet.getTimestamp("UPDATED_AT");
            MqSendOptions options = new MqSendOptions();
            String sendMode = resultSet.getString("SEND_MODE");
            String shardingKey = resultSet.getString("SHARDING_KEY");
            int storedDelayLevel = resultSet.getInt("DELAY_LEVEL");
            Integer delayLevel = resultSet.wasNull() ? null : storedDelayLevel;
            long storedTimeout = resultSet.getLong("SEND_TIMEOUT_MS");
            if (!resultSet.wasNull()) {
                options.setTimeoutMillis(storedTimeout);
            }
            String headers = resultSet.getString("HEADERS");
            if (StringUtils.hasText(headers)) {
                try {
                    options.setHeaders(MAPPER.readValue(headers, HEADER_TYPE));
                } catch (JsonProcessingException ex) {
                    throw new SQLException("Invalid Outbox headers JSON", ex);
                }
            }
            if ("ORDERLY".equals(sendMode) && StringUtils.hasText(shardingKey)) {
                options.setShardingKey(shardingKey);
            } else if ("DELAY".equals(sendMode) && delayLevel != null) {
                options.setDelay(MqDelay.level(delayLevel));
            } else if (!"NORMAL".equals(sendMode)) {
                throw new SQLException("Invalid Outbox send mode or required options");
            }
            return new MqOutboxEvent(
                    resultSet.getString("MESSAGE_ID"),
                    Base64.getDecoder().decode(resultSet.getString("PAYLOAD")),
                    resultSet.getString("TOPIC"),
                    resultSet.getString("TAG"),
                    resultSet.getString("BUSINESS_KEY"),
                    options,
                    MqOutboxStatus.valueOf(resultSet.getString("STATUS")),
                    resultSet.getInt("RETRY_COUNT"),
                    createdAt == null ? null : createdAt.toLocalDateTime(),
                    updatedAt == null ? null : updatedAt.toLocalDateTime());
        }
    }
}
