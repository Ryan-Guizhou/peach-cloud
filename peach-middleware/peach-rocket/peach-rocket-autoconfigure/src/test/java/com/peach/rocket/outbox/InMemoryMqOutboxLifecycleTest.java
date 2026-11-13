package com.peach.rocket.outbox;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 验证内存 Outbox 有界容量、原子认领与租约 token 保护。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/11
 */
class InMemoryMqOutboxLifecycleTest {

    @Test
    void shouldOnlyAllowOneConcurrentClaimAndFenceExpiredOwner() throws InterruptedException {
        MutableClock clock = new MutableClock();
        InMemoryMqOutboxStore store = new InMemoryMqOutboxStore(2, 1024,
                Duration.ofHours(1), Duration.ofMinutes(5), clock);
        store.save(event("one"));
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(12);
        AtomicInteger claims = new AtomicInteger();
        for (int i = 0; i < 12; i++) {
            Thread.ofVirtual().start(() -> {
                try {
                    start.await();
                    if (!store.claimPending(1).isEmpty()) {
                        claims.incrementAndGet();
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        done.await();
        assertThat(claims.get()).isEqualTo(1);
        clock.advance(Duration.ofMinutes(6));
        MqOutboxClaim second = store.claimPending(1).get(0);
        assertThatThrownBy(() -> store.markSent(new MqOutboxClaim(event("one"), "stale-owner")))
                .isInstanceOf(IllegalStateException.class);
        store.markSent(second);
        assertThat(store.claimPending(1)).isEmpty();
    }

    @Test
    void shouldNeverEvictPendingAndMustBoundPayloads() {
        MutableClock clock = new MutableClock();
        InMemoryMqOutboxStore store = new InMemoryMqOutboxStore(1, 1,
                Duration.ofHours(1), Duration.ofMinutes(5), clock);
        store.save(event("one"));
        assertThatThrownBy(() -> store.save(event("two")))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("capacity");
        assertThatThrownBy(() -> store.save(new MqOutboxEvent("large", new byte[]{1, 2},
                "t", null, "k", null, MqOutboxStatus.INIT, 0, LocalDateTime.now(), LocalDateTime.now())))
                .isInstanceOf(IllegalArgumentException.class);

        MqOutboxClaim claim = store.claimPending(1).get(0);
        store.markSent(claim);
        clock.advance(Duration.ofHours(2));
        store.save(event("two"));
        assertThat(store.claimPending(5)).hasSize(1);
    }

    private static MqOutboxEvent event(String id) {
        LocalDateTime now = LocalDateTime.now();
        return new MqOutboxEvent(id, new byte[]{1}, "topic", null, id,
                null, MqOutboxStatus.INIT, 0, now, now);
    }

    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-11-11T06:00:00Z");

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }
}
