package com.peach.scheduler.rocket.jdbc;

import java.time.ZoneId;

import org.springframework.stereotype.Indexed;

import com.peach.rocket.idempotent.MqIdempotentContext;
import com.peach.rocket.idempotent.MqIdempotentStore;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 调度 RocketMQ JDBC 幂等存储。
 *
 * <p>通过 MQ_CONSUME_RECORD 记录消费状态，避免同一执行命令被重复处理。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
@Indexed
public class SchedulerJdbcMqIdempotentStore implements MqIdempotentStore {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 创建 JDBC 幂等存储。
     *
     * @param jdbcTemplate JDBC 操作模板。
     */
    public SchedulerJdbcMqIdempotentStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
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
