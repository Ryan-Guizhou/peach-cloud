package com.peach.rocket.jdbc;

import com.peach.rocket.core.MqDelay;
import com.peach.rocket.core.MqSendOptions;
import com.peach.rocket.outbox.MqOutboxEvent;
import com.peach.rocket.outbox.MqOutboxStatus;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * JDBC Outbox 顺序与延迟发送选项持久化验证。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/13
 */
class JdbcMqOutboxSendOptionsTest {

    @Test
    void shouldPersistOrderlyShardKeyTimeoutAndHeaders() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        MqSendOptions options = new MqSendOptions();
        options.setShardingKey("order-1");
        options.setTimeoutMillis(4500L);
        options.setHeaders(Map.of("traceparent", "parent-1"));
        new JdbcMqOutboxStore(jdbc).save(event("ordered", options));

        verify(jdbc).update(contains("SHARDING_KEY,DELAY_LEVEL,HEADERS,SEND_TIMEOUT_MS"),
                eq("ordered"), eq("orders"), eq("created"), eq("ordered"), anyString(),
                eq("ORDERLY"), eq("order-1"), isNull(), contains("traceparent"), eq(4500L),
                eq("INIT"), eq(0), any(Timestamp.class), any(Timestamp.class), any(Timestamp.class));
    }

    @Test
    void shouldPersistDelayLevelWithoutShardKey() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        MqSendOptions options = new MqSendOptions();
        options.setDelay(MqDelay.duration(java.time.Duration.ofSeconds(5)));
        new JdbcMqOutboxStore(jdbc).save(event("delayed", options));

        verify(jdbc).update(contains("DELAY_LEVEL"),
                eq("delayed"), eq("orders"), eq("created"), eq("delayed"), anyString(),
                eq("DELAY"), isNull(), eq(2), isNull(), isNull(),
                eq("INIT"), eq(0), any(Timestamp.class), any(Timestamp.class), any(Timestamp.class));
    }

    private MqOutboxEvent event(String id, MqSendOptions options) {
        LocalDateTime now = LocalDateTime.now();
        return new MqOutboxEvent(id, new byte[]{1}, "orders", "created", id, options,
                MqOutboxStatus.INIT, 0, now, now);
    }
}
