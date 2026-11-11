package com.peach.redis.manager;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.peach.redis.common.tool.RedisDao;
import com.peach.redis.config.MultiCacheConfig;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 验证失效通知与在途 Redis 读取竞争时不会恢复已经失效的 L1。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/11 13:31
 */
class MultiCacheInvalidationRaceTest {

    @Test
    @SuppressWarnings("unchecked")
    void shouldNotRepopulateL1AfterConcurrentInvalidation() throws InterruptedException {
        RedisTemplate<Object, Object> redis = mock(RedisTemplate.class);
        ValueOperations<Object, Object> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(redis.getExpire("cache:key", TimeUnit.MILLISECONDS)).thenReturn(5_000L);

        CountDownLatch readStarted = new CountDownLatch(1);
        CountDownLatch releaseRead = new CountDownLatch(1);
        AtomicInteger reads = new AtomicInteger();
        when(values.get("cache:key")).thenAnswer(invocation -> {
            if (reads.getAndIncrement() == 0) {
                readStarted.countDown();
                if (!releaseRead.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Test Redis read timeout");
                }
                return "old-value";
            }
            return null;
        });

        MultiCache cache = new MultiCache("cache", redis, Caffeine.newBuilder().build(),
                new MultiCacheConfig(), mock(RedisDao.class));
        AtomicReference<Object> resolved = new AtomicReference<>();
        Thread reader = Thread.ofVirtual().start(() -> resolved.set(cache.get("key", String.class)));
        assertThat(readStarted.await(5, TimeUnit.SECONDS)).isTrue();
        cache.clearLocal("key");
        releaseRead.countDown();
        reader.join(5_000L);

        assertThat(reader.isAlive()).isFalse();
        assertThat(resolved.get()).isNull();
        assertThat(cache.getLocalCache().getIfPresent("key")).isNull();
        assertThat(reads.get()).isEqualTo(2);
    }
}
