package com.peach.redission.delayqueue.quickstart;

import com.peach.redission.delayqueue.core.ConsumerTask;
import com.peach.redission.delayqueue.v2.DelayQueueV2Properties;
import com.peach.redission.delayqueue.v2.RedisDelayQueueV2;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.redisson.Redisson;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.redisson.config.Config;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 用 GitHub Actions Redis 7.2 服务容器执行不可跳过的延迟队列 V2 Lua 集成验收。
 * 默认 Maven 全量验证不启动服务容器；只有专用 workflow 设置 RUN_REDIS_V2_IT 才运行。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/10
 */
@EnabledIfEnvironmentVariable(named = "RUN_REDIS_V2_IT", matches = "true")
class DelayQueueV2ServerIntegrationTest {

    private static RedissonClient redisson;
    private static RedisDelayQueueV2 queue;

    @BeforeAll
    static void setUp() {
        String host = System.getenv().getOrDefault("REDIS_IT_HOST", "127.0.0.1");
        String port = System.getenv().getOrDefault("REDIS_IT_PORT", "6379");
        Config config = new Config();
        config.useSingleServer().setAddress("redis://" + host + ":" + port).setTimeout(4000);
        redisson = Redisson.create(config);
        DelayQueueV2Properties properties = new DelayQueueV2Properties();
        properties.setEnabled(true);
        properties.setLease(Duration.ofSeconds(3));
        properties.setMaxAttempts(2);
        properties.setRetryInitial(Duration.ofMillis(1));
        properties.setRetryMax(Duration.ofMillis(10));
        queue = new RedisDelayQueueV2(redisson, properties);
    }

    @AfterAll
    static void tearDown() {
        if (queue != null) {
            queue.close();
        }
        if (redisson != null) {
            redisson.shutdown();
        }
    }

    @Test
    void atomicallyEnqueuesOnceAndOnlyCurrentOwnerCanAcknowledge() {
        String topic = newTopic();
        assertThat(queue.offer(topic, "job-1", "original", Duration.ZERO)).isTrue();
        assertThat(queue.offer(topic, "job-1", "duplicate", Duration.ZERO)).isFalse();
        var claimed = queue.claim(topic);
        assertThat(claimed).hasSize(1);
        assertThat(claimed.get(0).payload()).isEqualTo("original");
        assertThat(queue.claim(topic)).isEmpty();
        assertThat(queue.acknowledge(topic, "job-1", claimed.get(0).fencingToken() + 1)).isFalse();
        assertThat(queue.acknowledge(topic, "job-1", claimed.get(0).fencingToken())).isTrue();
        assertThat(queue.offer(topic, "job-1", "again", Duration.ZERO)).isFalse();
        assertThat(queue.status(topic).completed()).isEqualTo(1);
    }

    @Test
    void retriesThenDeadLettersAndReplaysUsingStableId() throws InterruptedException {
        String topic = newTopic();
        assertThat(queue.offer(topic, "job-2", "retry-value", Duration.ZERO)).isTrue();
        var first = queue.claim(topic).get(0);
        assertThat(queue.fail(topic, first)).isEqualTo(RedisDelayQueueV2.FailureOutcome.RETRY_SCHEDULED);
        Thread.sleep(50);
        var second = queue.claim(topic).get(0);
        assertThat(second.attempts()).isEqualTo(2);
        assertThat(second.fencingToken()).isGreaterThan(first.fencingToken());
        assertThat(queue.fail(topic, second)).isEqualTo(RedisDelayQueueV2.FailureOutcome.DEAD_LETTER);
        assertThat(queue.deadLetterPayload(topic, "job-2")).isEqualTo("retry-value");
        assertThat(queue.status(topic).deadLetters()).isEqualTo(1);
        assertThat(queue.replayDeadLetter(topic, "job-2", Duration.ZERO)).isTrue();
        var replay = queue.claim(topic).get(0);
        assertThat(replay.payload()).isEqualTo("retry-value");
        assertThat(queue.acknowledge(topic, replay.id(), replay.fencingToken())).isTrue();
    }

    @Test
    void reclaimRejectsTheStaleWorkerFencingToken() {
        String topic = newTopic();
        assertThat(queue.offer(topic, "job-3", "lease-value", Duration.ZERO)).isTrue();
        var first = queue.claim(topic).get(0);
        redisson.getScript(StringCodec.INSTANCE).eval(RScript.Mode.READ_WRITE,
                "return redis.call('ZADD', KEYS[1], 0, ARGV[1])",
                RScript.ReturnType.INTEGER,
                List.of("peach:dq2:{" + topic + "}:processing"), "job-3");
        assertThat(queue.reclaimExpired(topic)).isEqualTo(1);
        var next = queue.claim(topic).get(0);
        assertThat(next.fencingToken()).isGreaterThan(first.fencingToken());
        assertThat(queue.acknowledge(topic, first.id(), first.fencingToken())).isFalse();
        assertThat(queue.acknowledge(topic, next.id(), next.fencingToken())).isTrue();
    }

    @Test
    void pollingExecutesBusinessCallbackThenAcknowledges() {
        String topic = newTopic();
        AtomicInteger calls = new AtomicInteger();
        ConsumerTask task = new ConsumerTask() {
            @Override
            public void execute(String content) {
                assertThat(content).isEqualTo("callback-body");
                calls.incrementAndGet();
            }

            @Override
            public String topic() {
                return topic;
            }
        };
        assertThat(queue.offer(topic, "job-4", "callback-body", Duration.ZERO)).isTrue();
        assertThat(queue.pollOnce(topic, task)).isEqualTo(1);
        assertThat(calls.get()).isEqualTo(1);
        assertThat(queue.status(topic).processing()).isZero();
        assertThat(queue.status(topic).completed()).isEqualTo(1);
    }

    private static String newTopic() {
        return "dq2-it-" + UUID.randomUUID().toString().replace("-", "");
    }
}
