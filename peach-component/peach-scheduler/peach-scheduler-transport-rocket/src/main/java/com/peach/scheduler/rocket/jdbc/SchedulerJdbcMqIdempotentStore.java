package com.peach.scheduler.rocket.jdbc;

import org.springframework.stereotype.Indexed;

import com.peach.rocket.idempotent.MqIdempotentContext;
import com.peach.rocket.idempotent.MqIdempotentStore;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 调度模块相关说明。
 *
 * <p>调度模块相关说明。
 * 调度模块相关说明。
 * 调度模块相关说明。
 * 调度模块相关说明。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
@Indexed
public class SchedulerJdbcMqIdempotentStore implements MqIdempotentStore {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 创建相关对象。
     *
     * @param jdbcTemplate 参数说明
     */
    public SchedulerJdbcMqIdempotentStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 继承接口定义。
     */
    @Override
    public boolean tryStart(MqIdempotentContext context) {
        try {
            jdbcTemplate.update(
                    "INSERT INTO MQ_CONSUME_RECORD(IDEMPOTENT_KEY,CONSUMER_GROUP,TOPIC,TAG,BUSINESS_KEY,"
                            + "MESSAGE_ID,STATUS,CONSUME_COUNT,CREATED_AT,UPDATED_AT) "
                            + "VALUES(?,?,?,?,?,?,'PROCESSING',1,NOW(3),NOW(3))",
                    context.getIdempotentKey(),
                    context.getConsumerGroup(),
                    context.getTopic(),
                    context.getTag(),
                    context.getBusinessKey(),
                    context.getMessageId());
            return true;
        } catch (DuplicateKeyException ex) {
            if (isSuccess(context)) {
                return false;
            }
            Duration expire = context.getExpire() == null ? Duration.ofHours(24) : context.getExpire();
            LocalDateTime staleBefore = LocalDateTime.now().minus(expire);
            return jdbcTemplate.update(
                    "UPDATE MQ_CONSUME_RECORD SET STATUS='PROCESSING',CONSUME_COUNT=CONSUME_COUNT+1,"
                            + "LAST_ERROR=NULL,UPDATED_AT=NOW(3) WHERE IDEMPOTENT_KEY=? AND CONSUMER_GROUP=? "
                            + "AND (STATUS='FAILED' OR (STATUS='PROCESSING' AND UPDATED_AT<?))",
                    context.getIdempotentKey(),
                    context.getConsumerGroup(),
                    Timestamp.valueOf(staleBefore)) == 1;
        }
    }

    /**
     * 继承接口定义。
     */
    @Override
    public void markSuccess(MqIdempotentContext context) {
        jdbcTemplate.update(
                "UPDATE MQ_CONSUME_RECORD SET STATUS='SUCCESS',LAST_ERROR=NULL,UPDATED_AT=NOW(3) "
                        + "WHERE IDEMPOTENT_KEY=? AND CONSUMER_GROUP=?",
                context.getIdempotentKey(), context.getConsumerGroup());
    }

    /**
     * 继承接口定义。
     */
    @Override
    public void markFailed(MqIdempotentContext context) {
        jdbcTemplate.update(
                "UPDATE MQ_CONSUME_RECORD SET STATUS='FAILED',UPDATED_AT=NOW(3) "
                        + "WHERE IDEMPOTENT_KEY=? AND CONSUMER_GROUP=?",
                context.getIdempotentKey(), context.getConsumerGroup());
    }

    /**
     * 继承接口定义。
     */
    @Override
    public boolean isSuccess(MqIdempotentContext context) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM MQ_CONSUME_RECORD "
                        + "WHERE IDEMPOTENT_KEY=? AND CONSUMER_GROUP=? AND STATUS='SUCCESS'",
                Integer.class, context.getIdempotentKey(), context.getConsumerGroup());
        return count != null && count > 0;
    }
}
