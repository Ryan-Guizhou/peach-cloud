package com.peach.rocket.outbox;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 验证 Outbox 认领状态不会被过期令牌覆盖，重试预算耗尽后可受控重放。
 
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/13
 */
class InMemoryMqOutboxStoreStateTest {

    @Test
    void completedEventCannotBeReopenedByItsOldClaim() {
        InMemoryMqOutboxStore store = new InMemoryMqOutboxStore();
        store.save(event("sent"));
        MqOutboxClaim claim = store.claimPending(1).getFirst();
        store.markSent(claim);
        assertThatThrownBy(() -> store.markFailed(claim))
                .isInstanceOf(IllegalStateException.class);
        assertThat(store.claimPending(10)).isEmpty();
        assertThat(store.replay("sent")).isFalse();
    }

    @Test
    void shouldStopAfterTwentyAttemptsAndAllowExplicitReplay() {
        MutableClock clock = new MutableClock();
        InMemoryMqOutboxStore store = new InMemoryMqOutboxStore(
                8, 1024, Duration.ofHours(3), Duration.ofMinutes(5), clock);
        store.save(event("retry"));
        for (int attempt = 0; attempt < 20; attempt++) {
            MqOutboxClaim claim = store.claimPending(1).getFirst();
            store.markFailed(claim);
            clock.advance(Duration.ofMinutes(6));
        }
        assertThat(store.claimPending(1)).isEmpty();
        assertThat(store.replay("retry")).isTrue();
        assertThat(store.claimPending(1)).singleElement()
                .extracting(claim -> claim.event().retryCount()).isEqualTo(0);
    }

    private static MqOutboxEvent event(String id) {
        LocalDateTime now = LocalDateTime.now();
        return new MqOutboxEvent(id, new byte[]{1}, "topic", null, id, null,
                MqOutboxStatus.INIT, 0, now, now);
    }

    private static final class MutableClock extends Clock {
        private Instant instant = Instant.parse("2026-11-13T00:00:00Z");

        void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public Instant instant() {
            return instant;
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
