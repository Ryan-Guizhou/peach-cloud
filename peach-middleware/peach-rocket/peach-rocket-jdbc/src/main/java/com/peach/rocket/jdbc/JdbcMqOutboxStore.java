package com.peach.rocket.jdbc;

import com.peach.rocket.autoconfigure.PeachRocketProperties;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.util.ArrayList;
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
    private final PeachRocketProperties properties;

    private static final String INSERT_SQL =
            "INSERT INTO MQ_OUTBOX_EVENT(MESSAGE_ID,TOPIC,TAG,BUSINESS_KEY,PAYLOAD,SEND_MODE,"
                    + "SHARDING_KEY,DELAY_LEVEL,HEADERS,SEND_TIMEOUT_MS,STATUS,"
                    + "RETRY_COUNT,NEXT_RETRY_TIME,CREATED_AT,UPDATED_AT)"
                    + " VALUES(?,?,?,?,?,?,?,?,?,?,?,?,NOW(3),NOW(3),NOW(3))";

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, String>> HEADER_TYPE = new TypeReference<>() { };

    private final JdbcTemplate jdbcTemplate;

    /**
     * 创建 JDBC outbox 存储。
     *
     * @param jdbcTemplate JDBC 操作模板。
     */
    public JdbcMqOutboxStore(JdbcTemplate jdbcTemplate) {
        this(jdbcTemplate, new PeachRocketProperties());
    }

    public JdbcMqOutboxStore(JdbcTemplate jdbcTemplate, PeachRocketProperties properties) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate");
        this.properties = Objects.requireNonNull(properties, "properties");
    }

    /**
     * 保存待发送消息。
     */
    @Override
    public void save(MqOutboxEvent event) {
        assertTransaction(false);
        saveEvent(event);
    }

    private void saveEvent(MqOutboxEvent event) {
        Objects.requireNonNull(event, "event");
        if (!StringUtils.hasText(event.messageId()) || event.messageId().length() > 64
                || !StringUtils.hasText(event.topic()) || event.topic().length() > 128
                || event.body() == null || event.body().length > properties.getProducer().getMaxPayloadBytes()) {
            throw new MqException("Invalid Outbox identifier, topic or encoded payload size");
        }
        MqSendOptions options = event.options();
        if (options != null && (options.getTimeoutMillis() < 0
                || options.getTimeoutMillis() >= properties.getJdbc().getOutboxLease().toMillis())) {
            throw new MqException("Outbox timeout must be shorter than its lease");
        }
        if (properties.isProduction() && options != null && StringUtils.hasText(options.getShardingKey())) {
            throw new MqException("Production Outbox does not support strict orderly delivery");
        }
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
                0);
    }

    /**
     * 历史入口会丢失认领令牌，已停止支持；调用方必须迁移到 claimPending 和带令牌的确认方法。
     *
     * @deprecated 使用 {@link #claimPending(int)} 保留认领凭据。
     */
    @Override
    @Deprecated
    public List<MqOutboxEvent> findPending(int batchSize) {
        throw new UnsupportedOperationException(
                "JDBC Outbox requires claimPending and token-scoped markSent/markFailed; migrate legacy findPending callers");
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
        String batchClaimToken = "rocket-outbox-" + UUID.randomUUID();
        jdbcTemplate.update(
                "UPDATE MQ_OUTBOX_EVENT SET STATUS=?,CLAIMED_BY=?,CLAIMED_AT=NOW(3),UPDATED_AT=NOW(3) "
                        + "WHERE STATUS IN (?,?) AND NEXT_RETRY_TIME<=NOW(3) "
                        + "ORDER BY UPDATED_AT ASC LIMIT ?",
                MqOutboxStatus.SENDING.name(),
                batchClaimToken,
                MqOutboxStatus.INIT.name(),
                MqOutboxStatus.RETRY.name(),
                boundedBatch);
        List<String> corrupt = new ArrayList<>();
        List<MqOutboxEvent> events = jdbcTemplate.query(
                "SELECT MESSAGE_ID,TOPIC,TAG,BUSINESS_KEY,PAYLOAD,SEND_MODE,SHARDING_KEY,"
                        + "DELAY_LEVEL,HEADERS,SEND_TIMEOUT_MS,STATUS,RETRY_COUNT,CREATED_AT,UPDATED_AT "
                        + "FROM MQ_OUTBOX_EVENT WHERE STATUS=? AND CLAIMED_BY=? "
                        + "ORDER BY CLAIMED_AT ASC LIMIT ?",
                (rs, row) -> {
                    try {
                        return new OutboxRowMapper().mapRow(rs, row);
                    }
                    catch (RuntimeException | SQLException invalid) {
                        corrupt.add(rs.getString("MESSAGE_ID"));
                        return null;
                    }
                },
                MqOutboxStatus.SENDING.name(),
                batchClaimToken,
                boundedBatch);
        for (String id : corrupt) {
            jdbcTemplate.update("UPDATE MQ_OUTBOX_EVENT SET STATUS='FAILED',LAST_ERROR='INVALID_STORED_EVENT',"
                    + "CLAIMED_BY=NULL,CLAIMED_AT=NULL,UPDATED_AT=NOW(3) WHERE MESSAGE_ID=? AND STATUS='SENDING' AND CLAIMED_BY=?",
                    id, batchClaimToken);
        }
        return events.stream().filter(Objects::nonNull).map(event -> new MqOutboxClaim(event, batchClaimToken)).toList();
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
                        + "WHERE MESSAGE_ID=? AND STATUS=? AND CLAIMED_BY=? AND CLAIMED_AT>DATE_SUB(NOW(3), INTERVAL ? MICROSECOND)",
                MqOutboxStatus.SENT.name(), claim.event().messageId(),
                MqOutboxStatus.SENDING.name(), token, leaseMicros());
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
                        + "RETRY_COUNT=LEAST(20,RETRY_COUNT+1),"
                        + "NEXT_RETRY_TIME=DATE_ADD(NOW(3), INTERVAL LEAST(300, POW(2,RETRY_COUNT)) SECOND),"
                        + "CLAIMED_BY=NULL,CLAIMED_AT=NULL,UPDATED_AT=NOW(3) "
                        + "WHERE MESSAGE_ID=? AND STATUS=? AND CLAIMED_BY=? AND CLAIMED_AT>DATE_SUB(NOW(3), INTERVAL ? MICROSECOND)",
                MAX_RETRY_COUNT,
                MqOutboxStatus.FAILED.name(),
                MqOutboxStatus.RETRY.name(),
                claim.event().messageId(), MqOutboxStatus.SENDING.name(), token, leaseMicros());
        if (updated != 1) {
            throw new IllegalStateException("Outbox claim is stale when marking failed");
        }
    }

    @Override
    public boolean isDurable() {
        return true;
    }
    @Override
    public boolean supportsOwnerFencing() {
        return true;
    }
    @Override
    public boolean supportsLeaseRenewal() {
        return true;
    }
    @Override
    public boolean renew(MqOutboxClaim claim) {
        return jdbcTemplate.update("UPDATE MQ_OUTBOX_EVENT SET CLAIMED_AT=NOW(3),UPDATED_AT=NOW(3) "
                + "WHERE MESSAGE_ID=? AND STATUS='SENDING' AND CLAIMED_BY=? "
                + "AND CLAIMED_AT>DATE_SUB(NOW(3), INTERVAL ? MICROSECOND)",
                claim.event().messageId(), requireClaimToken(claim), leaseMicros()) == 1;
    }

    private long leaseMicros() {
        return properties.getJdbc().getOutboxLease().toNanos() / 1000;
    }

    @Override
    public void saveStandalone(MqOutboxEvent event) {
        assertTransaction(true);
        saveEvent(event);
    }

    private void assertTransaction(boolean standalone) {
        boolean active = TransactionSynchronizationManager.isActualTransactionActive();
        boolean sameResource = JdbcLocalTransactionVerifier.participates(jdbcTemplate.getDataSource());
        if (active && !sameResource) {
            throw new MqException("Outbox transaction must own the configured DataSource connection");
        }
        if (!standalone && (properties.isProduction() || properties.getOutbox().isRequireTransaction()) && (!active || !sameResource)) {
            throw new MqException("Outbox requires an active transaction on its configured DataSource");
        }
    }

    @Override
    public int cleanupTerminal(int limit) {
        if (limit < 1) {
            throw new IllegalArgumentException("Cleanup limit must be positive");
        }
        return jdbcTemplate.update("DELETE FROM MQ_OUTBOX_EVENT WHERE STATUS IN ('SENT','FAILED') AND CLAIMED_BY IS NULL "
                + "AND UPDATED_AT<DATE_SUB(NOW(3), INTERVAL ? SECOND) LIMIT ?",
                properties.getJdbc().getRetention().toSeconds(), Math.min(limit, MAX_BATCH_SIZE));
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
                        + "WHERE MESSAGE_ID=? AND STATUS=? AND CLAIMED_BY IS NULL",
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
                        + "RETRY_COUNT=LEAST(20,RETRY_COUNT+1),"
                        + "NEXT_RETRY_TIME=DATE_ADD(NOW(3), INTERVAL LEAST(300, POW(2,RETRY_COUNT)) SECOND),"
                        + "CLAIMED_BY=NULL,CLAIMED_AT=NULL,UPDATED_AT=NOW(3) "
                        + "WHERE MESSAGE_ID=? AND STATUS=? AND CLAIMED_BY IS NULL",
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
                        + "CLAIMED_BY=NULL,CLAIMED_AT=NULL,UPDATED_AT=NOW(3) WHERE MESSAGE_ID=? AND STATUS=? AND CLAIMED_BY IS NULL",
                MqOutboxStatus.RETRY.name(),
                messageId,
                MqOutboxStatus.FAILED.name()) == 1;
    }

    /**
     * 恢复超时未完成发送的认领记录。
     */
    private void recoverStaleClaims() {
        jdbcTemplate.update(
                "UPDATE MQ_OUTBOX_EVENT SET STATUS=CASE WHEN RETRY_COUNT+1>=20 THEN 'FAILED' ELSE ? END,RETRY_COUNT=LEAST(20,RETRY_COUNT+1),CLAIMED_BY=NULL,CLAIMED_AT=NULL,NEXT_RETRY_TIME=NOW(3),"
                        + "UPDATED_AT=NOW(3) WHERE STATUS=? AND COALESCE(CLAIMED_AT,UPDATED_AT)<DATE_SUB(NOW(3), INTERVAL ? MICROSECOND) "
                        + "ORDER BY CLAIMED_AT ASC LIMIT ?",
                MqOutboxStatus.RETRY.name(),
                MqOutboxStatus.SENDING.name(),
                leaseMicros(),
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
