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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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


    @Test
    void independentWorkerCanReclaimAfterOwnerStopsWithoutAcknowledging() throws Exception {
        String topic = newTopic();
        Config firstConfig = new Config();
        firstConfig.useSingleServer().setAddress("redis://127.0.0.1:6379");
        Config secondConfig = new Config();
        secondConfig.useSingleServer().setAddress("redis://127.0.0.1:6379");

        RedissonClient firstClient = Redisson.create(firstConfig);
        RedissonClient secondClient = Redisson.create(secondConfig);
        DelayQueueV2Properties firstProperties = integrationProperties();
        DelayQueueV2Properties secondProperties = integrationProperties();
        try (RedisDelayQueueV2 first = new RedisDelayQueueV2(firstClient, firstProperties);
             RedisDelayQueueV2 second = new RedisDelayQueueV2(secondClient, secondProperties)) {
            assertThat(first.offer(topic, "recover-1", "unfinished-work", Duration.ZERO)).isTrue();
            RedisDelayQueueV2.ClaimedMessage abandoned = first.claim(topic).get(0);
            first.close(); // No ACK: simulate a worker stopping while the lease remains in Redis.

            Thread.sleep(3600);
            assertThat(second.reclaimExpired(topic)).isEqualTo(1);
            RedisDelayQueueV2.ClaimedMessage recovered = second.claim(topic).get(0);
            assertThat(recovered.id()).isEqualTo(abandoned.id());
            assertThat(recovered.fencingToken()).isGreaterThan(abandoned.fencingToken());
            assertThat(second.acknowledge(topic, abandoned.id(), abandoned.fencingToken())).isFalse();
            assertThat(second.acknowledge(topic, recovered.id(), recovered.fencingToken())).isTrue();
            assertThat(second.status(topic).completed()).isEqualTo(1);
            assertThat(second.status(topic).processing()).isZero();
        } finally {
            secondClient.shutdown();
            firstClient.shutdown();
        }
    }

    @Test
    void twoIndependentWorkersProcessBoundedBatchWithoutLostOrDuplicateCompletions() throws Exception {
        String topic = newTopic();
        final int messages = 64;
        Config firstConfig = new Config();
        firstConfig.useSingleServer().setAddress("redis://127.0.0.1:6379");
        Config secondConfig = new Config();
        secondConfig.useSingleServer().setAddress("redis://127.0.0.1:6379");
        RedissonClient firstClient = Redisson.create(firstConfig);
        RedissonClient secondClient = Redisson.create(secondConfig);
        ExecutorService threads = Executors.newFixedThreadPool(2);
        try (RedisDelayQueueV2 first = new RedisDelayQueueV2(firstClient, integrationProperties());
             RedisDelayQueueV2 second = new RedisDelayQueueV2(secondClient, integrationProperties())) {
            for (int i = 0; i < messages; i++) {
                assertThat(first.offer(topic, "job-" + i, "payload-" + i, Duration.ZERO)).isTrue();
                assertThat(first.offer(topic, "job-" + i, "duplicate", Duration.ZERO)).isFalse();
            }
            java.util.Set<String> processed = java.util.concurrent.ConcurrentHashMap.newKeySet();
            AtomicInteger invocations = new AtomicInteger();
            ConsumerTask consumer = new ConsumerTask() {
                @Override
                public void execute(String content) {
                    invocations.incrementAndGet();
                    assertThat(processed.add(content)).as("Unexpected duplicate: " + content).isTrue();
                }
                @Override
                public String topic() {
                    return topic;
                }
            };
            CompletableFuture<Void> firstFuture = CompletableFuture.runAsync(() -> {
                for (int i = 0; i < 8; i++) {
                    first.pollOnce(topic, consumer);
                }
            }, threads);
            CompletableFuture<Void> secondFuture = CompletableFuture.runAsync(() -> {
                for (int i = 0; i < 8; i++) {
                    second.pollOnce(topic, consumer);
                }
            }, threads);
            CompletableFuture.allOf(firstFuture, secondFuture).get(25, TimeUnit.SECONDS);
            assertThat(processed).hasSize(messages);
            assertThat(invocations.get()).isEqualTo(messages);
            assertThat(second.status(topic).scheduled()).isZero();
            assertThat(second.status(topic).processing()).isZero();
            assertThat(second.status(topic).completed()).isEqualTo(messages);
        } finally {
            threads.shutdownNow();
            secondClient.shutdown();
            firstClient.shutdown();
        }
    }

    @Test
    void forciblyKilledIndependentJvmLeavesMessageReclaimableByAnotherWorker() throws Exception {
        String topic = newTopic();
        String id = "process-crash-1";
        String payload = "worker-must-recover";
        Path marker = Files.createTempFile("peach-dq2-kill-marker-", ".txt");
        Path childOutput = Files.createTempFile("peach-dq2-kill-output-", ".txt");
        Process child = null;
        RedissonClient newOwnerClient = null;

        try {
            assertThat(queue.offer(topic, id, payload, Duration.ZERO)).isTrue();
            String executable = Path.of(System.getProperty("java.home"), "bin", "java").toString();
            String classPath = System.getProperty("java.class.path");
            assertThat(classPath).as("Test runner must expose a runnable child JVM classpath").isNotBlank();
            ProcessBuilder builder = new ProcessBuilder(
                    executable, "-cp", classPath,
                    "com.peach.redission.delayqueue.quickstart.DelayQueueV2CrashWorker",
                    topic, marker.toString());
            builder.redirectErrorStream(true);
            builder.redirectOutput(childOutput.toFile());
            child = builder.start();

            boolean claimed = false;
            for (int attempt = 0; attempt < 120; attempt++) {
                if (Files.size(marker) > 0) {
                    claimed = true;
                    break;
                }
                if (!child.isAlive()) {
                    break;
                }
                Thread.sleep(100);
            }
            assertThat(claimed)
                    .as("Independent worker must claim before kill; child output: "
                            + Files.readString(childOutput, StandardCharsets.UTF_8))
                    .isTrue();

            String[] fields = Files.readString(marker, StandardCharsets.UTF_8).trim().split("\\|", -1);
            assertThat(fields).hasSize(3);
            assertThat(fields[0]).isEqualTo(id);
            assertThat(fields[2]).isEqualTo(payload);
            long staleToken = Long.parseLong(fields[1]);
            assertThat(queue.status(topic).processing()).isEqualTo(1L);

            child.destroyForcibly(); // Real subprocess kill: no ACK and no graceful queue.close().
            assertThat(child.waitFor(10, TimeUnit.SECONDS)).isTrue();
            assertThat(child.isAlive()).isFalse();

            Config recoveredConfig = new Config();
            recoveredConfig.useSingleServer().setAddress("redis://127.0.0.1:6379");
            newOwnerClient = Redisson.create(recoveredConfig);
            try (RedisDelayQueueV2 recovered = new RedisDelayQueueV2(newOwnerClient, integrationProperties())) {
                // Claims have a three-second lease; a fresh owner may reclaim after expiry.
                Thread.sleep(3600);
                assertThat(recovered.reclaimExpired(topic)).isEqualTo(1L);
                List<RedisDelayQueueV2.ClaimedMessage> messages = recovered.claim(topic);
                assertThat(messages).hasSize(1);
                RedisDelayQueueV2.ClaimedMessage message = messages.get(0);
                assertThat(message.id()).isEqualTo(id);
                assertThat(message.payload()).isEqualTo(payload);
                assertThat(message.fencingToken()).isGreaterThan(staleToken);
                assertThat(recovered.acknowledge(topic, id, staleToken)).isFalse();
                assertThat(recovered.acknowledge(topic, id, message.fencingToken())).isTrue();
                assertThat(recovered.status(topic).processing()).isZero();
                assertThat(recovered.status(topic).scheduled()).isZero();
                assertThat(recovered.status(topic).completed()).isEqualTo(1L);
            }
        } finally {
            if (child != null && child.isAlive()) {
                child.destroyForcibly();
                child.waitFor(10, TimeUnit.SECONDS);
            }
            if (newOwnerClient != null) {
                newOwnerClient.shutdown();
            }
            Files.deleteIfExists(marker);
            Files.deleteIfExists(childOutput);
        }
    }

    private static DelayQueueV2Properties integrationProperties() {
        DelayQueueV2Properties properties = new DelayQueueV2Properties();
        properties.setEnabled(true);
        properties.setLease(Duration.ofSeconds(3));
        properties.setBatchSize(16);
        properties.setMaxAttempts(2);
        properties.setRetryInitial(Duration.ofMillis(1));
        properties.setRetryMax(Duration.ofMillis(10));
        return properties;
    }

    private static String newTopic() {
        return "dq2-it-" + UUID.randomUUID().toString().replace("-", "");
    }
}
