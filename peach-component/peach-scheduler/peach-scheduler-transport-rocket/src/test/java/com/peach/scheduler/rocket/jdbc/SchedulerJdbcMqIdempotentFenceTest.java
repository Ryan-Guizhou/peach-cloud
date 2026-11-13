package com.peach.scheduler.rocket.jdbc;

import com.peach.rocket.idempotent.MqIdempotentClaim;
import com.peach.rocket.idempotent.MqIdempotentContext;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证 JDBC 消费 ACK 必须附带当前 owner token。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/12
 */
class SchedulerJdbcMqIdempotentFenceTest {

    @Test
    void shouldOnlyUpdateMatchingOwnerToken() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        SchedulerJdbcMqIdempotentStore store = new SchedulerJdbcMqIdempotentStore(jdbc);
        MqIdempotentContext context = new MqIdempotentContext(
                "key-1", "group-1", "orders", "created", "key-1", "msg-1", Duration.ofHours(1));
        MqIdempotentClaim claim = MqIdempotentClaim.acquired("current-owner");
        when(jdbc.update(anyString(), anyLong(), eq("key-1"), eq("group-1"), eq("current-owner")))
                .thenReturn(1);

        store.markSuccess(context, claim);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbc).update(sql.capture(), anyLong(), eq("key-1"), eq("group-1"), eq("current-owner"));
        assertThat(sql.getValue()).contains("OWNER_TOKEN=?").contains("LEASE_UNTIL>NOW(3)");
        assertThat(store.supportsOwnerFencing()).isTrue();
    }

    @Test
    void shouldRejectStaleTokenWhenDatabaseDoesNotUpdate() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        SchedulerJdbcMqIdempotentStore store = new SchedulerJdbcMqIdempotentStore(jdbc);
        MqIdempotentContext context = new MqIdempotentContext(
                "key-1", "group-1", "orders", "created", "key-1", "msg-1", Duration.ofHours(1));

        assertThatThrownBy(() -> store.markFailed(context, MqIdempotentClaim.acquired("stale-owner")))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("Stale");
        assertThatThrownBy(() -> store.markSuccess(context, MqIdempotentClaim.acquired(null)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
