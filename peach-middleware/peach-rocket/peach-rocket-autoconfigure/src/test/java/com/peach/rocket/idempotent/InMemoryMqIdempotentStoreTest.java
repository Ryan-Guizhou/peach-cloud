package com.peach.rocket.idempotent;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * MQ 幂等内存存储同键原子领取测试。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/11
 */
class InMemoryMqIdempotentStoreTest {

    @Test
    void shouldAllowOnlyOneConcurrentClaimOfSameKey() throws Exception {
        InMemoryMqIdempotentStore store = new InMemoryMqIdempotentStore();
        MqIdempotentContext context = context();
        CountDownLatch ready = new CountDownLatch(64);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> claims = new ArrayList<>();

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < 64; i++) {
                claims.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    return store.tryStart(context);
                }));
            }
            ready.await();
            start.countDown();
            int acquired = 0;
            for (Future<Boolean> claim : claims) {
                if (claim.get()) {
                    acquired++;
                }
            }
            assertEquals(1, acquired);
        }

        store.markSuccess(context);
        assertTrue(store.isSuccess(context));
        assertFalse(store.tryStart(context));
    }

    private static MqIdempotentContext context() {
        return new MqIdempotentContext("id-1", "group-1",
                "topic", "tag", "business", "message", Duration.ofMinutes(1));
    }
}
