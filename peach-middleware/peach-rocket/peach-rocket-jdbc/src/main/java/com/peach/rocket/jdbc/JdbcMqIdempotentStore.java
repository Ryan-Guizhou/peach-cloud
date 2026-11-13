package com.peach.rocket.jdbc;

import java.time.ZoneId;


import com.peach.rocket.idempotent.MqIdempotentClaim;
import com.peach.rocket.idempotent.MqIdempotentContext;
import com.peach.rocket.idempotent.MqIdempotentStore;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 通用 RocketMQ JDBC 幂等存储。
 *
 * <p>通过 MQ_CONSUME_RECORD 记录消费状态，避免同一执行命令被重复处理。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
public class JdbcMqIdempotentStore implements MqIdempotentStore {

    private static final int PROCESSING_LEASE_SECONDS = 300;

    private final JdbcTemplate jdbcTemplate;

    /**
     * 创建 JDBC 幂等存储。
     *
     * @param jdbcTemplate JDBC 操作模板。
     */
    public JdbcMqIdempotentStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 数据库原子认领；使用 owner token 防止旧处理者覆盖新处理者。
     *
     * @param context 幂等上下文
     * @return 认领结果
     */
    @Override
    public MqIdempotentClaim claim(MqIdempotentContext context) {
        String token = UUID.randomUUID().toString();
        try {
            jdbcTemplate.update(
                    "INSERT INTO MQ_CONSUME_RECORD(IDEMPOTENT_KEY,CONSUMER_GROUP,TOPIC,TAG,BUSINESS_KEY,"
                            + "MESSAGE_ID,STATUS,CONSUME_COUNT,CREATED_AT,UPDATED_AT,OWNER_TOKEN,LEASE_UNTIL) "
                            + "VALUES(?,?,?,?,?,?,'PROCESSING',1,NOW(3),NOW(3),?,"
                            + "DATE_ADD(NOW(3), INTERVAL ? SECOND))",
                    context.idempotentKey(), context.consumerGroup(), context.topic(), context.tag(),
                    context.businessKey(), context.messageId(), token, PROCESSING_LEASE_SECONDS);
            return MqIdempotentClaim.acquired(token);
        } catch (DuplicateKeyException ignored) {
            if (isSuccess(context)) {
                return MqIdempotentClaim.succeeded();
            }
            int updated = jdbcTemplate.update(
                    "UPDATE MQ_CONSUME_RECORD SET STATUS='PROCESSING',OWNER_TOKEN=?,"
                            + "LEASE_UNTIL=DATE_ADD(NOW(3), INTERVAL ? SECOND),"
                            + "CONSUME_COUNT=CONSUME_COUNT+1,LAST_ERROR=NULL,UPDATED_AT=NOW(3) "
                            + "WHERE IDEMPOTENT_KEY=? AND CONSUMER_GROUP=? "
                            + "AND (STATUS='FAILED' OR (STATUS='PROCESSING' "
                            + "AND (LEASE_UNTIL IS NULL OR LEASE_UNTIL<NOW(3))))",
                    token, PROCESSING_LEASE_SECONDS, context.idempotentKey(), context.consumerGroup());
            return updated == 1 ? MqIdempotentClaim.acquired(token) : MqIdempotentClaim.inProgress();
        }
    }

    @Override
    public void markSuccess(MqIdempotentContext context, MqIdempotentClaim claim) {
        String token = requireOwnerToken(claim);
        int updated = jdbcTemplate.update(
                "UPDATE MQ_CONSUME_RECORD SET STATUS='SUCCESS',OWNER_TOKEN=NULL,"
                        + "LEASE_UNTIL=NULL,LAST_ERROR=NULL,UPDATED_AT=NOW(3) "
                        + "WHERE IDEMPOTENT_KEY=? AND CONSUMER_GROUP=? "
                        + "AND STATUS='PROCESSING' AND OWNER_TOKEN=?",
                context.idempotentKey(), context.consumerGroup(), token);
        if (updated != 1) {
            throw new IllegalStateException("Stale MQ idempotent owner cannot confirm success");
        }
    }

    @Override
    public void markFailed(MqIdempotentContext context, MqIdempotentClaim claim) {
        String token = requireOwnerToken(claim);
        int updated = jdbcTemplate.update(
                "UPDATE MQ_CONSUME_RECORD SET STATUS='FAILED',OWNER_TOKEN=NULL,"
                        + "LEASE_UNTIL=NULL,UPDATED_AT=NOW(3) "
                        + "WHERE IDEMPOTENT_KEY=? AND CONSUMER_GROUP=? "
                        + "AND STATUS='PROCESSING' AND OWNER_TOKEN=?",
                context.idempotentKey(), context.consumerGroup(), token);
        if (updated != 1) {
            throw new IllegalStateException("Stale MQ idempotent owner cannot mark failure");
        }
    }

    @Override
    public boolean supportsOwnerFencing() {
        return true;
    }

    private String requireOwnerToken(MqIdempotentClaim claim) {
        if (claim == null || claim.status() != MqIdempotentClaim.Status.ACQUIRED
                || claim.ownerToken() == null || claim.ownerToken().isBlank()) {
            throw new IllegalArgumentException("MQ JDBC idempotent owner token is required");
        }
        return claim.ownerToken();
    }

    /**
     * 尝试进入消息处理状态。
     */
    @Override
    public boolean tryStart(MqIdempotentContext context) {
        try {
            jdbcTemplate.update(
                    "INSERT INTO MQ_CONSUME_RECORD(IDEMPOTENT_KEY,CONSUMER_GROUP,TOPIC,TAG,BUSINESS_KEY,"
                            + "MESSAGE_ID,STATUS,CONSUME_COUNT,CREATED_AT,UPDATED_AT) "
                            + "VALUES(?,?,?,?,?,?,'PROCESSING',1,NOW(3),NOW(3))",
                    context.idempotentKey(),
                    context.consumerGroup(),
                    context.topic(),
                    context.tag(),
                    context.businessKey(),
                    context.messageId());
            return true;
        } catch (DuplicateKeyException ex) {
            if (isSuccess(context)) {
                return false;
            }
            Duration expire = context.expire() == null ? Duration.ofHours(24) : context.expire();
            LocalDateTime staleBefore = LocalDateTime.now(ZoneId.systemDefault()).minus(expire);
            return jdbcTemplate.update(
                    "UPDATE MQ_CONSUME_RECORD SET STATUS='PROCESSING',CONSUME_COUNT=CONSUME_COUNT+1,"
                            + "LAST_ERROR=NULL,UPDATED_AT=NOW(3) WHERE IDEMPOTENT_KEY=? AND CONSUMER_GROUP=? "
                            + "AND (STATUS='FAILED' OR (STATUS='PROCESSING' AND UPDATED_AT<?))",
                    context.idempotentKey(),
                    context.consumerGroup(),
                    Timestamp.valueOf(staleBefore)) == 1;
        }
    }

    /**
     * 标记消息处理成功。
     */
    @Override
    public void markSuccess(MqIdempotentContext context) {
        jdbcTemplate.update(
                "UPDATE MQ_CONSUME_RECORD SET STATUS='SUCCESS',LAST_ERROR=NULL,UPDATED_AT=NOW(3) "
                        + "WHERE IDEMPOTENT_KEY=? AND CONSUMER_GROUP=?",
                context.idempotentKey(), context.consumerGroup());
    }

    /**
     * 标记消息处理失败。
     */
    @Override
    public void markFailed(MqIdempotentContext context) {
        jdbcTemplate.update(
                "UPDATE MQ_CONSUME_RECORD SET STATUS='FAILED',UPDATED_AT=NOW(3) "
                        + "WHERE IDEMPOTENT_KEY=? AND CONSUMER_GROUP=?",
                context.idempotentKey(), context.consumerGroup());
    }

    /**
     * 判断消息是否已成功消费。
     */
    @Override
    public boolean isSuccess(MqIdempotentContext context) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM MQ_CONSUME_RECORD "
                        + "WHERE IDEMPOTENT_KEY=? AND CONSUMER_GROUP=? AND STATUS='SUCCESS'",
                Integer.class, context.idempotentKey(), context.consumerGroup());
        return count != null && count > 0;
    }
}
