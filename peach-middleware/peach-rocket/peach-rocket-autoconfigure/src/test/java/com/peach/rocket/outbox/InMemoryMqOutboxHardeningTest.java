package com.peach.rocket.outbox;

import com.peach.rocket.autoconfigure.PeachRocketProperties;
import com.peach.rocket.idempotent.InMemoryMqIdempotentStore;
import com.peach.rocket.idempotent.MqIdempotentClaim;
import com.peach.rocket.idempotent.MqIdempotentContext;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 验证过期令牌、幂等键边界与租约防护，不保留无令牌状态更新。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/13
 */
class InMemoryMqOutboxHardeningTest {

    @Test
    void completedIdempotentRecordRejectsStaleOwner() {
        InMemoryMqIdempotentStore store = new InMemoryMqIdempotentStore();
        MqIdempotentContext context = new MqIdempotentContext(
                "key", "group", "t", null, null, "1", Duration.ofHours(1));
        MqIdempotentClaim owner = store.claim(context);
        store.markSuccess(context, owner);
        assertThatThrownBy(() -> store.markFailed(context, owner))
                .isInstanceOf(IllegalStateException.class);
        assertThat(store.claim(context).status()).isEqualTo(MqIdempotentClaim.Status.SUCCEEDED);
    }


    @Test
    void compositeIdempotentKeysCannotCollideOnDelimiter() {
        InMemoryMqIdempotentStore store = new InMemoryMqIdempotentStore();
        MqIdempotentContext first = new MqIdempotentContext(
                "b:c", "a", "t", null, null, "1", Duration.ofHours(1));
        MqIdempotentContext second = new MqIdempotentContext(
                "c", "a:b", "t", null, null, "2", Duration.ofHours(1));
        assertThat(store.claim(first).status()).isEqualTo(MqIdempotentClaim.Status.ACQUIRED);
        assertThat(store.claim(second).status()).isEqualTo(MqIdempotentClaim.Status.ACQUIRED);
    }

    @Test
    void staleRenewalPreventsBrokerSend() {
        MqOutboxStore store = mock(MqOutboxStore.class);
        RocketMQTemplate template = mock(RocketMQTemplate.class);
        PeachRocketProperties properties = new PeachRocketProperties();
        LocalDateTime now = LocalDateTime.now();
        MqOutboxEvent event = new MqOutboxEvent(
                "id", new byte[]{1}, "topic", null, null, null, MqOutboxStatus.SENDING, 0, now, now);
        when(store.claimPending(1)).thenReturn(List.of(new MqOutboxClaim(event, "owner")), List.of());
        when(store.supportsLeaseRenewal()).thenReturn(true);
        when(store.renew(any())).thenReturn(false);

        new MqOutboxDispatcher(store, template, properties).dispatch();

        verifyNoInteractions(template);
        verify(store, never()).markSent(any(MqOutboxClaim.class));
    }

    @Test
    void invalidOutboxOwnerCannotReplaceCurrentClaim() {
        InMemoryMqOutboxStore store = new InMemoryMqOutboxStore();
        LocalDateTime now = LocalDateTime.now();
        store.save(new MqOutboxEvent("id", new byte[]{1}, "t", null, null, null, MqOutboxStatus.INIT, 0, now, now));
        MqOutboxClaim claim = store.claimPending(1).getFirst();
        MqOutboxClaim invalid = new MqOutboxClaim(claim.event(), "foreign-owner");
        assertThatThrownBy(() -> store.markSent(invalid)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> store.markFailed(invalid)).isInstanceOf(IllegalStateException.class);
        store.markSent(claim);
    }
}
