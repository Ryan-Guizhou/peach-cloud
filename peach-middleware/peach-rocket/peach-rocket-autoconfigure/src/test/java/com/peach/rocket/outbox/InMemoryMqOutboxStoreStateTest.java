package com.peach.rocket.outbox;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证内存 Outbox 同一消息的状态变更不会丢失重试次数。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/11 11:30
 */
class InMemoryMqOutboxStoreStateTest {

    @Test
    void shouldNotReopenSentEventAfterFailure() {
        InMemoryMqOutboxStore store = new InMemoryMqOutboxStore();
        store.save(event("sent"));
        store.markSent("sent");
        store.markFailed("sent");
        assertThat(store.findPending(10)).isEmpty();
        assertThat(store.replay("sent")).isFalse();
    }

    @Test
    void shouldKeepAllConcurrentFailedAttempts() throws InterruptedException {
        InMemoryMqOutboxStore store = new InMemoryMqOutboxStore();
        store.save(event("retry"));
        int workers = 16;
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(workers);
        AtomicInteger errors = new AtomicInteger();
        for (int i = 0; i < workers; i++) {
            Thread.ofVirtual().start(() -> {
                try {
                    start.await();
                    store.markFailed("retry");
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    errors.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        done.await();
        assertThat(errors.get()).isZero();
        assertThat(store.findPending(1)).singleElement()
                .extracting(MqOutboxEvent::retryCount).isEqualTo(workers);
    }

    private static MqOutboxEvent event(String id) {
        LocalDateTime now = LocalDateTime.now();
        return new MqOutboxEvent(id, new byte[]{1}, "topic", null, id, null,
                MqOutboxStatus.INIT, 0, now, now);
    }
}
