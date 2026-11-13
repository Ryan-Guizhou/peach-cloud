package com.peach.rocket.idempotent;

import java.time.Duration;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 验证内存幂等存储 owner fencing 和有界容量。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/12
 */
class InMemoryMqIdempotentFenceTest {

    @Test
    void shouldRejectStaleOwnerAfterExpiredClaimIsReplaced() throws InterruptedException {
        InMemoryMqIdempotentStore store = new InMemoryMqIdempotentStore(2);
        MqIdempotentContext firstContext = context("order-1", Duration.ofMillis(20));
        MqIdempotentClaim first = store.claim(firstContext);
        assertThat(first.status()).isEqualTo(MqIdempotentClaim.Status.ACQUIRED);

        Thread.sleep(80);
        MqIdempotentContext secondContext = context("order-1", Duration.ofMinutes(1));
        MqIdempotentClaim second = store.claim(secondContext);
        assertThat(second.ownerToken()).isNotEqualTo(first.ownerToken());

        assertThatThrownBy(() -> store.markFailed(firstContext, first))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("Stale");
        assertThatThrownBy(() -> store.markSuccess(firstContext, first))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("Stale");
        store.markSuccess(secondContext, second);
        assertThat(store.claim(secondContext).status()).isEqualTo(MqIdempotentClaim.Status.SUCCEEDED);
    }

    @Test
    void shouldRejectCapacityOverflowInsteadOfEvictingProcessingRecord() {
        InMemoryMqIdempotentStore store = new InMemoryMqIdempotentStore(1);
        MqIdempotentContext first = context("first", Duration.ofMinutes(1));
        MqIdempotentClaim claim = store.claim(first);
        assertThatThrownBy(() -> store.claim(context("second", Duration.ofMinutes(1))))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("capacity");
        store.markFailed(first, claim);
        assertThat(store.claim(context("second", Duration.ofMinutes(1))).status())
                .isEqualTo(MqIdempotentClaim.Status.ACQUIRED);
    }

    private MqIdempotentContext context(String key, Duration expire) {
        return new MqIdempotentContext(key, "group", "orders", "created",
                key, "message-" + key, expire);
    }
}
