package com.peach.redission.delayqueue.quickstart;

import com.peach.redission.delayqueue.config.DelayQueueProperties;
import com.peach.redission.delayqueue.core.RedisDelayQueue;
import org.junit.jupiter.api.Test;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 验证延迟队列的关闭操作不会因并行 Redis 预检查而被 Java monitor 阻塞。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/11
 */
class DelayQueueLifecycleLockTest {

    @Test
    void closeMustNotWaitForRedisPreflightMonitor() throws Exception {
        RedissonClient redisson = mock(RedissonClient.class);
        @SuppressWarnings("unchecked")
        RBlockingQueue<String> ready = mock(RBlockingQueue.class);
        @SuppressWarnings("unchecked")
        RDelayedQueue<String> delayed = mock(RDelayedQueue.class);
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        when(redisson.<String>getBlockingQueue("slow-topic")).thenReturn(ready);
        when(redisson.getDelayedQueue(ready)).thenReturn(delayed);
        @SuppressWarnings("unchecked")
        RMap<String, String> processing = mock(RMap.class);
        @SuppressWarnings("unchecked")
        RMap<String, String> deadLetters = mock(RMap.class);
        when(redisson.<String, String>getMap("slow-topic:processing")).thenReturn(processing);
        when(redisson.<String, String>getMap("slow-topic-dead-letter")).thenReturn(deadLetters);
        when(delayed.size()).thenAnswer(invocation -> {
            entered.countDown();
            if (!release.await(4, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Preflight did not finish");
            }
            return 0;
        });

        DelayQueueProperties properties = new DelayQueueProperties();
        properties.setPollInterval(Duration.ofMillis(50));
        RedisDelayQueue queue = new RedisDelayQueue(redisson, properties);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread starter = Thread.ofVirtual().start(() -> {
            try {
                queue.start("slow-topic", new com.peach.redission.delayqueue.core.ConsumerTask() {
                    @Override
                    public String topic() {
                        return "slow-topic";
                    }

                    @Override
                    public void execute(String payload) {
                    }
                });
            } catch (Throwable exception) {
                failure.set(exception);
            }
        });
        assertThat(entered.await(2, TimeUnit.SECONDS)).isTrue();

        CountDownLatch finished = new CountDownLatch(1);
        Thread closer = Thread.ofVirtual().start(() -> {
            try {
                queue.close();
            } finally {
                finished.countDown();
            }
        });
        try {
            assertThat(finished.await(2, TimeUnit.SECONDS)).isTrue();
        } finally {
            release.countDown();
        }
        closer.join(TimeUnit.SECONDS.toMillis(3));
        starter.join(TimeUnit.SECONDS.toMillis(3));
        assertThat(starter.isAlive()).isFalse();
        assertThat(failure.get()).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> queue.start("slow-topic", mock(com.peach.redission.delayqueue.core.ConsumerTask.class)))
                .isInstanceOf(IllegalStateException.class);
    }
}
