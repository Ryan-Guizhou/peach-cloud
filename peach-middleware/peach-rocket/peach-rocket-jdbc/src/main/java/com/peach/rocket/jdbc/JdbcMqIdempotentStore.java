package com.peach.rocket.jdbc;

import com.peach.rocket.autoconfigure.PeachRocketProperties;
import com.peach.rocket.idempotent.MqIdempotentClaim;
import com.peach.rocket.idempotent.MqIdempotentContext;
import com.peach.rocket.idempotent.MqIdempotentStore;
import java.time.Duration;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StringUtils;

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

    private final PeachRocketProperties properties;

    private final JdbcTemplate jdbcTemplate;


    /**
     * 创建采用统一租约配置的 JDBC 幂等存储。
     *
     * @param jdbcTemplate JDBC 操作模板
     * @param properties RocketMQ 统一配置
     */
    public JdbcMqIdempotentStore(JdbcTemplate jdbcTemplate, PeachRocketProperties properties) {
        this.jdbcTemplate = jdbcTemplate;
        this.properties = properties;
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
                            + "DATE_ADD(NOW(3), INTERVAL ? MICROSECOND))",
                    context.idempotentKey(), context.consumerGroup(), context.topic(), context.tag(),
                    context.businessKey(), context.messageId(), token, leaseMicros());
            return MqIdempotentClaim.acquired(token);
        } catch (DuplicateKeyException ignored) {
            if (completed(context)) {
                return MqIdempotentClaim.succeeded();
            }
            int updated = jdbcTemplate.update(
                    "UPDATE MQ_CONSUME_RECORD SET STATUS='PROCESSING',OWNER_TOKEN=?,"
                            + "LEASE_UNTIL=DATE_ADD(NOW(3), INTERVAL ? MICROSECOND),"
                            + "CONSUME_COUNT=CONSUME_COUNT+1,LAST_ERROR=NULL,UPDATED_AT=NOW(3) "
                            + "WHERE IDEMPOTENT_KEY=? AND CONSUMER_GROUP=? "
                            + "AND (STATUS='FAILED' OR (STATUS='PROCESSING' "
                            + "AND LEASE_UNTIL<=NOW(3)) OR (STATUS='SUCCESS' AND EXPIRES_AT<=NOW(3)))",
                    token, leaseMicros(), context.idempotentKey(), context.consumerGroup());
            return updated == 1 ? MqIdempotentClaim.acquired(token) : MqIdempotentClaim.inProgress();
        }
    }

    @Override
    public void markSuccess(MqIdempotentContext context, MqIdempotentClaim claim) {
        String token = requireOwnerToken(claim);
        int updated = jdbcTemplate.update(
                "UPDATE MQ_CONSUME_RECORD SET STATUS='SUCCESS',OWNER_TOKEN=NULL,"
                        + "LEASE_UNTIL=NULL,LAST_ERROR=NULL,EXPIRES_AT=DATE_ADD(NOW(3), INTERVAL ? MICROSECOND),UPDATED_AT=NOW(3) "
                        + "WHERE IDEMPOTENT_KEY=? AND CONSUMER_GROUP=? "
                        + "AND STATUS='PROCESSING' AND OWNER_TOKEN=? AND LEASE_UNTIL>NOW(3)",
                ttlMicros(context), context.idempotentKey(), context.consumerGroup(), token);
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
                        + "AND STATUS='PROCESSING' AND OWNER_TOKEN=? AND LEASE_UNTIL>NOW(3)",
                context.idempotentKey(), context.consumerGroup(), token);
        if (updated != 1) {
            throw new IllegalStateException("Stale MQ idempotent owner cannot mark failure");
        }
    }

    @Override
    public boolean supportsLeaseRenewal() {
        return true;
    }

    @Override
    public boolean supportsOwnerFencing() {
        return true;
    }

    @Override
    public boolean isDurable() {
        return true;
    }

    @Override
    public boolean renew(MqIdempotentContext context, MqIdempotentClaim claim) {
        return jdbcTemplate.update("UPDATE MQ_CONSUME_RECORD SET LEASE_UNTIL=DATE_ADD(NOW(3), INTERVAL ? MICROSECOND),UPDATED_AT=NOW(3) "
                + "WHERE IDEMPOTENT_KEY=? AND CONSUMER_GROUP=? AND STATUS='PROCESSING' AND OWNER_TOKEN=? AND LEASE_UNTIL>NOW(3)",
                leaseMicros(), context.idempotentKey(), context.consumerGroup(), requireOwnerToken(claim)) == 1;
    }

    @Override
    public int cleanupTerminal(int limit) {
        if (limit < 1) {
            throw new IllegalArgumentException("Cleanup limit must be positive");
        }
        return jdbcTemplate.update("DELETE FROM MQ_CONSUME_RECORD WHERE OWNER_TOKEN IS NULL AND "
                + "((STATUS='SUCCESS' AND EXPIRES_AT<=NOW(3)) OR (STATUS='FAILED' AND UPDATED_AT<DATE_SUB(NOW(3), INTERVAL ? SECOND))) LIMIT ?",
                properties.getJdbc().getRetention().toSeconds(), Math.min(500, limit));
    }

    private long leaseMicros() {
        return properties.getJdbc().getProcessingLease().toNanos() / 1000;
    }

    private long ttlMicros(MqIdempotentContext context) {
        Duration ttl = context.expire() == null ? Duration.ofHours(24) : context.expire();
        if (ttl.isNegative() || ttl.isZero()) {
            throw new IllegalArgumentException("Idempotent TTL must be positive");
        }
        return ttl.toNanos() / 1000;
    }

    private String requireOwnerToken(MqIdempotentClaim claim) {
        if (claim == null || claim.status() != MqIdempotentClaim.Status.ACQUIRED
                || !StringUtils.hasText(claim.ownerToken())) {
            throw new IllegalArgumentException("MQ JDBC idempotent owner token is required");
        }
        return claim.ownerToken();
    }


    /**
     * 校验去重窗口内已完成的消息。
     *
     * @param context 幂等上下文
     * @return 是否已经成功消费
     */
    private boolean completed(MqIdempotentContext context) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM MQ_CONSUME_RECORD "
                        + "WHERE IDEMPOTENT_KEY=? AND CONSUMER_GROUP=? AND STATUS='SUCCESS' AND EXPIRES_AT>NOW(3)",
                Integer.class, context.idempotentKey(), context.consumerGroup());
        return count != null && count > 0;
    }
}
