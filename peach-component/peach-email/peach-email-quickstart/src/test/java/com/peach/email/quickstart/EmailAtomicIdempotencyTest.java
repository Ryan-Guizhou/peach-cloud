package com.peach.email.quickstart;

import com.peach.email.core.EmailContext;
import com.peach.email.core.EmailFailureKind;
import com.peach.email.core.EmailMessage;
import com.peach.email.core.EmailTransport;
import com.peach.email.core.SendResult;
import com.peach.email.idempotency.SimpleIdempotencyStore;
import com.peach.email.retry.SimpleRetryPolicy;
import com.peach.email.router.ProviderRouter;
import com.peach.email.service.EmailSendService;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证同键并发邮件不会重复调用外部 SMTP，且幂等保护有容量/时间边界。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/11 13:31
 */
class EmailAtomicIdempotencyTest {

    @Test
    void sameKeyMustNotSendTwiceWhileFirstCallIsInFlight() throws InterruptedException {
        ProviderRouter router = mock(ProviderRouter.class);
        EmailTransport transport = mock(EmailTransport.class);
        EmailContext context = mock(EmailContext.class);
        when(router.getProviderNames()).thenReturn(Set.of("primary"));
        when(router.orderByPriority(any())).thenReturn(List.of("primary"));
        when(router.getTransport("primary")).thenReturn(transport);
        when(router.getContext("primary")).thenReturn(context);
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        when(transport.send(any(), any())).thenAnswer(invocation -> {
            entered.countDown();
            if (!release.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Test SMTP timeout");
            }
            return new SendResult("primary", "msg-1", 1L, true, null);
        });
        EmailSendService service = new EmailSendService(router, new SimpleIdempotencyStore(),
                new SimpleRetryPolicy(1, 0), "primary");
        EmailMessage message = message("business-key");
        AtomicReference<SendResult> first = new AtomicReference<>();
        Thread sender = Thread.ofVirtual().start(() -> first.set(service.sendAuto(message)));
        assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue();
        SendResult parallel = service.sendAuto(message);
        assertThat(parallel.isSuccess()).isFalse();
        assertThat(parallel.getFailureKind()).isEqualTo(EmailFailureKind.DELIVERY_UNKNOWN);
        release.countDown();
        sender.join(5_000L);
        assertThat(sender.isAlive()).isFalse();
        assertThat(first.get().isSuccess()).isTrue();
        assertThat(service.sendAuto(message).isSuccess()).isTrue();
        verify(transport, times(1)).send(any(), any());
    }

    @Test
    void unknownStateShouldBlockReclaimUntilExpiryAndCapacityShouldBeBounded() {
        MutableClock clock = new MutableClock();
        SimpleIdempotencyStore store = new SimpleIdempotencyStore(Duration.ofHours(1), 1, clock);
        String token = store.tryClaim("mail-one");
        store.markUnknown("mail-one", token);

        assertThat(store.exists("mail-one")).isFalse();
        assertThat(store.tryClaim("mail-one")).isNull();
        assertThatThrownBy(() -> store.tryClaim("mail-two"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("capacity");

        clock.advance(Duration.ofHours(2));
        assertThat(store.tryClaim("mail-two")).isNotNull();
    }

    @Test
    void staleTokenCannotCompleteAnotherClaim() {
        SimpleIdempotencyStore store = new SimpleIdempotencyStore();
        String token = store.tryClaim("key");
        assertThatThrownBy(() -> store.markSent("key", "wrong",
                new SendResult("p", "m", 1L, true, null)))
                .isInstanceOf(IllegalStateException.class);
        store.markSent("key", token, new SendResult("p", "m", 1L, true, null));
        assertThat(store.exists("key")).isTrue();
    }

    @Test
    void independentKeysShouldRespectCapacityAndReleaseClaimTokensConcurrently() throws Exception {
        SimpleIdempotencyStore store = new SimpleIdempotencyStore(Duration.ofMinutes(10), 16);
        java.util.concurrent.CountDownLatch begin = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.atomic.AtomicInteger accepted = new java.util.concurrent.atomic.AtomicInteger();
        java.util.concurrent.atomic.AtomicInteger rejected = new java.util.concurrent.atomic.AtomicInteger();
        java.util.concurrent.ConcurrentMap<String, String> tokens = new java.util.concurrent.ConcurrentHashMap<>();
        try (java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
            java.util.List<java.util.concurrent.Future<?>> futures = new java.util.ArrayList<>();
            for (int i = 0; i < 64; i++) {
                final String key = "parallel-" + i;
                futures.add(executor.submit(() -> {
                    try {
                        begin.await();
                        String token = store.tryClaim(key);
                        if (token != null) {
                            tokens.put(key, token);
                            accepted.incrementAndGet();
                        }
                    } catch (IllegalStateException exception) {
                        rejected.incrementAndGet();
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                    }
                }));
            }
            begin.countDown();
            for (var future : futures) {
                future.get();
            }
        }
        assertThat(accepted.get()).isEqualTo(16);
        assertThat(rejected.get()).isEqualTo(48);

        var admitted = tokens.entrySet().iterator().next();
        assertThat(store.tryClaim(admitted.getKey())).isNull();
        store.releaseClaim(admitted.getKey(), admitted.getValue());
        assertThat(store.tryClaim("after-release")).isNotNull();
    }

    private static EmailMessage message(String key) {
        return EmailMessage.builder().from("sender@example.invalid")
                .to(List.of("receiver@example.invalid"))
                .subject("Test").text("Test").idempotencyKey(key).build();
    }

    private static final class MutableClock extends Clock {
        private Instant current = Instant.parse("2026-11-11T00:00:00Z");

        void advance(Duration duration) {
            current = current.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return current;
        }
    }
}
