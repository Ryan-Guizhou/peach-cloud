package com.peach.redission.delayqueue.quickstart;

import com.peach.redission.delayqueue.autoconfigure.DelayQueueProperties;
import com.peach.redission.delayqueue.core.RedisDelayQueue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.api.RScript;
import org.redisson.client.codec.StringCodec;
import org.redisson.config.Config;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 独立 reliable 队列在真实 Redis 上验证幂等入队、fencing、租约回收、DLQ 与重放。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/10
 */
@Testcontainers(disabledWithoutDocker = true)
class DelayQueueReliabilityTest {

    @Container
    @SuppressWarnings("resource")
    static final GenericContainer<?> REDIS = new GenericContainer<>(
            DockerImageName.parse("redis:7.2-alpine")).withExposedPorts(6379);

    private RedissonClient redisson;
    private RedisDelayQueue queue;

    @BeforeEach
    void setUp() {
        Config config = new Config();
        config.useSingleServer().setAddress("redis://" + REDIS.getHost() + ":" + REDIS.getMappedPort(6379));
        redisson = Redisson.create(config);
        DelayQueueProperties options = new DelayQueueProperties();
        options.setEnabled(true);
        options.setLease(Duration.ofSeconds(3));
        options.setMaxAttempts(2);
        options.setRetryInitial(Duration.ofMillis(1));
        options.setRetryMax(Duration.ofMillis(10));
        options.setPollInterval(Duration.ofMillis(10));
        queue = new RedisDelayQueue(redisson, options);
    }

    @AfterEach
    void tearDown() {
        if (queue != null) {
            queue.close();
        }
        if (redisson != null) {
            redisson.shutdown();
        }
    }

    @Test
    void shouldEnqueueOnceAndRejectStaleAcknowledgment() {
        String topic = uniqueTopic();
        assertThat(queue.offer(topic, "job-1", "payload", Duration.ZERO)).isTrue();
        assertThat(queue.offer(topic, "job-1", "changed", Duration.ZERO)).isFalse();

        var claimed = queue.claim(topic);
        assertThat(claimed).hasSize(1);
        assertThat(claimed.get(0).payload()).isEqualTo("payload");
        long token = claimed.get(0).fencingToken();

        assertThat(queue.acknowledge(topic, "job-1", token + 1)).isFalse();
        assertThat(queue.acknowledge(topic, "job-1", token)).isTrue();
        assertThat(queue.offer(topic, "job-1", "duplicate", Duration.ZERO)).isFalse();
        assertThat(queue.status(topic).completed()).isEqualTo(1);
    }

    @Test
    void shouldMoveToDlqAndReplayStableId() throws Exception {
        String topic = uniqueTopic();
        assertThat(queue.offer(topic, "job-2", "timeout", Duration.ZERO)).isTrue();
        RedisDelayQueue.ClaimedMessage first = queue.claim(topic).get(0);
        assertThat(queue.fail(topic, first)).isEqualTo(RedisDelayQueue.FailureOutcome.RETRY_SCHEDULED);

        Thread.sleep(30);
        RedisDelayQueue.ClaimedMessage second = queue.claim(topic).get(0);
        assertThat(second.fencingToken()).isGreaterThan(first.fencingToken());
        assertThat(queue.fail(topic, second)).isEqualTo(RedisDelayQueue.FailureOutcome.DEAD_LETTER);

        assertThat(queue.deadLetterPayload(topic, "job-2")).isEqualTo("timeout");
        assertThat(queue.status(topic).deadLetters()).isEqualTo(1);
        assertThat(queue.offer(topic, "job-2", "conflict", Duration.ZERO)).isFalse();
        assertThat(queue.replayDeadLetter(topic, "job-2", Duration.ZERO)).isTrue();
        assertThat(queue.deadLetterPayload(topic, "job-2")).isNull();
        RedisDelayQueue.ClaimedMessage replay = queue.claim(topic).get(0);
        assertThat(replay.payload()).isEqualTo("timeout");
        assertThat(queue.acknowledge(topic, replay.id(), replay.fencingToken())).isTrue();
    }

    @Test
    void reclaimShouldInvalidatePreviousFencingToken() {
        String topic = uniqueTopic();
        assertThat(queue.offer(topic, "job-3", "recover", Duration.ZERO)).isTrue();
        RedisDelayQueue.ClaimedMessage first = queue.claim(topic).get(0);

        // Explicitly expire lease score in the test Redis without waiting 3 seconds.
        Long modified = redisson.getScript(StringCodec.INSTANCE).eval(RScript.Mode.READ_WRITE,
                "return redis.call('ZADD', KEYS[1], 0, ARGV[1])", RScript.ReturnType.INTEGER,
                List.of("peach:dq2:{" + topic + "}:processing"), "job-3");
        assertThat(modified).isNotNull();

        assertThat(queue.reclaimExpired(topic)).isEqualTo(1L);
        RedisDelayQueue.ClaimedMessage second = queue.claim(topic).get(0);
        assertThat(second.fencingToken()).isGreaterThan(first.fencingToken());
        assertThat(queue.acknowledge(topic, first.id(), first.fencingToken())).isFalse();
        assertThat(queue.acknowledge(topic, second.id(), second.fencingToken())).isTrue();
    }

    private static String uniqueTopic() {
        return "queue-" + UUID.randomUUID().toString().replace("-", "");
    }
}
