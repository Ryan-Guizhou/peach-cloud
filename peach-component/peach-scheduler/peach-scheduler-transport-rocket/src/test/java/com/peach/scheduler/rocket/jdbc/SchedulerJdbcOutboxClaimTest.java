package com.peach.scheduler.rocket.jdbc;

import com.peach.rocket.outbox.MqOutboxClaim;
import com.peach.rocket.outbox.MqOutboxEvent;
import com.peach.rocket.outbox.MqOutboxStatus;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证 Outbox ACK/FAIL 必须携带本次认领者令牌才能修改数据库。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/11 13:31
 */
class SchedulerJdbcOutboxClaimTest {

    @Test
    void markSentMustRequireAndMatchClaimToken() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        SchedulerJdbcMqOutboxStore store = new SchedulerJdbcMqOutboxStore(jdbc);
        MqOutboxClaim claim = new MqOutboxClaim(event(), "batch-claim-001");
        when(jdbc.update(anyString(), eq("SENT"), eq("message-1"),
                eq("SENDING"), eq("batch-claim-001"))).thenReturn(1);

        store.markSent(claim);

        verify(jdbc).update(anyString(), eq("SENT"), eq("message-1"),
                eq("SENDING"), eq("batch-claim-001"));
    }

    @Test
    void shouldRejectStaleClaimAndMissingToken() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        SchedulerJdbcMqOutboxStore store = new SchedulerJdbcMqOutboxStore(jdbc);

        assertThatThrownBy(() -> store.markSent(new MqOutboxClaim(event(), null)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> store.markSent(new MqOutboxClaim(event(), "expired-token")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("stale");
    }

    private static MqOutboxEvent event() {
        LocalDateTime now = LocalDateTime.now();
        return new MqOutboxEvent("message-1", new byte[]{1}, "topic", null, "business-1",
                null, MqOutboxStatus.SENDING, 0, now, now);
    }
}
