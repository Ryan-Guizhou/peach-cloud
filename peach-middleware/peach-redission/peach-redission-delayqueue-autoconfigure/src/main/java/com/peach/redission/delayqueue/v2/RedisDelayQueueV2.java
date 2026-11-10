package com.peach.redission.delayqueue.v2;

import com.peach.redission.delayqueue.core.ConsumerTask;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RMap;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 独立延迟队列 V2：在同一 Redis Cluster hash slot 内使用 Lua 原子状态转换。
 *
 * <p>与旧 RDelayedQueue/RBlockingQueue 完全分离，无自动迁移、无协议混读。
 * 消费为 at-least-once；租约与 fencing token 只保护 Redis ACK，不替代
 * 外部数据库/接口的幂等、fencing 校验或事务提交。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/10
 */
@Slf4j
public final class RedisDelayQueueV2 implements AutoCloseable {

    private static final String SCRIPT_ROOT = "lua/delayqueue-v2/";
    private static final String TOPIC_PATTERN = "[a-zA-Z0-9._:-]{1,96}";
    private static final String ID_PATTERN = "[a-zA-Z0-9._:-]{1,128}";

    private final RedissonClient redisson;
    private final DelayQueueV2Properties properties;
    private final RScript redisScripts;
    private final Map<String, String> scripts;
    private final ScheduledThreadPoolExecutor poller;
    private final ScheduledExecutorService heartbeat;
    private final Map<String, ScheduledFuture<?>> pollingTopics = new ConcurrentHashMap<>();
    private final Map<String, LeasedMessage> active = new ConcurrentHashMap<>();
    private volatile boolean closed;

    /**
     * 任务由 Redis Lua 操作原子认领，fencingToken 单调递增。
     */
    public record ClaimedMessage(String id, String payload, long fencingToken, long attempts) {
    }

    /**
     * 每个主题的当前状态，不包括已经过保留期的完成记录。
     */
    public record QueueStatus(long scheduled, long processing, long deadLetters, long completed) {
    }

    public enum FailureOutcome {
        STALE_LEASE, RETRY_SCHEDULED, DEAD_LETTER
    }

    private record LeasedMessage(String topic, ClaimedMessage message) {
    }

    public RedisDelayQueueV2(RedissonClient redisson, DelayQueueV2Properties properties) {
        this.redisson = Objects.requireNonNull(redisson, "redisson");
        this.properties = Objects.requireNonNull(properties, "properties");
        properties.validate();
        this.redisScripts = redisson.getScript(StringCodec.INSTANCE);
        this.scripts = Map.ofEntries(
                Map.entry("enqueue", script("enqueue.lua")),
                Map.entry("claim", script("claim.lua")),
                Map.entry("ack", script("ack.lua")),
                Map.entry("fail", script("fail.lua")),
                Map.entry("reclaim", script("reclaim.lua")),
                Map.entry("renew", script("renew.lua")),
                Map.entry("replay", script("replay.lua")),
                Map.entry("cancel", script("cancel.lua")),
                Map.entry("status", script("status.lua"))
        );
        AtomicInteger counter = new AtomicInteger(1);
        this.poller = new ScheduledThreadPoolExecutor(properties.getWorkerCount(), task -> {
            Thread thread = new Thread(task, "peach-dq2-poll-" + counter.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        });
        poller.setRemoveOnCancelPolicy(true);
        this.heartbeat = Executors.newSingleThreadScheduledExecutor(task -> {
            Thread thread = new Thread(task, "peach-dq2-heartbeat");
            thread.setDaemon(true);
            return thread;
        });
        long heartbeatInterval = Math.max(1000L, properties.getLease().toMillis() / 3);
        heartbeat.scheduleWithFixedDelay(this::renewSafely, heartbeatInterval,
                heartbeatInterval, TimeUnit.MILLISECONDS);
    }

    /**
     * 入队时要求应用提供稳定 ID，不使用时间戳作为重复判定依据。
     *
     * @return true 表示首次接受；false 表示队列/死信/完成保留期中已经存在
     */
    public boolean offer(String topic, String id, String payload, Duration delay) {
        ensureOpen();
        validateTopic(topic);
        validateId(id);
        Objects.requireNonNull(payload, "payload");
        if (payload.getBytes(StandardCharsets.UTF_8).length > properties.getMaxPayloadBytes()) {
            throw new IllegalArgumentException("Delay queue V2 payload exceeds configured maxPayloadBytes");
        }
        long delayMillis = positiveOrZero(delay, "delay");
        return evaluateLong("enqueue", keys(topic, "scheduled", "payload", "attempts", "dlq", "completed"),
                id, payload, delayMillis, properties.getCompletedRetention().toMillis()) == 1L;
    }

    /**
     * 原子认领：Redis 时间判定到期，移动 scheduled -> processing，
     * 同时生成递增的 fencing token。
     */
    public List<ClaimedMessage> claim(String topic) {
        ensureOpen();
        validateTopic(topic);
        List<Object> flat = evaluateList("claim",
                keys(topic, "scheduled", "processing", "tokens", "attempts", "payload", "fencing"),
                properties.getLease().toMillis(), properties.getBatchSize());
        if (flat.size() % 4 != 0) {
            throw new IllegalStateException("Invalid Redis delay queue V2 claim response");
        }
        List<ClaimedMessage> out = new ArrayList<>(flat.size() / 4);
        for (int i = 0; i < flat.size(); i += 4) {
            out.add(new ClaimedMessage(String.valueOf(flat.get(i)), String.valueOf(flat.get(i + 1)),
                    Long.parseLong(String.valueOf(flat.get(i + 2))),
                    Long.parseLong(String.valueOf(flat.get(i + 3)))));
        }
        return out;
    }

    /** 使用 fencing token 确认；旧租约持有者不能 ACK 新所有者的任务。 */
    public boolean acknowledge(String topic, String id, long fencingToken) {
        ensureOpen();
        validateTopic(topic);
        validateId(id);
        return evaluateLong("ack", keys(topic, "processing", "tokens", "payload", "attempts", "completed"),
                id, Long.toString(fencingToken)) == 1L;
    }

    /**
     * 业务失败后在脚本内原子转移至 scheduled 或 DLQ。
     * 即使回调重复执行，也不会为同一 ID 重复生成新的消息。
     */
    public FailureOutcome fail(String topic, ClaimedMessage message) {
        ensureOpen();
        validateTopic(topic);
        Objects.requireNonNull(message, "message");
        long retryDelay = backoffMillis(message.attempts());
        long result = evaluateLong("fail",
                keys(topic, "processing", "tokens", "scheduled", "attempts",
                        "payload", "dlq", "dlq-meta"),
                message.id(), Long.toString(message.fencingToken()), properties.getMaxAttempts(), retryDelay);
        return switch ((int) result) {
            case 0 -> FailureOutcome.STALE_LEASE;
            case 1 -> FailureOutcome.RETRY_SCHEDULED;
            case 2 -> FailureOutcome.DEAD_LETTER;
            default -> throw new IllegalStateException("Invalid delay queue V2 fail result: " + result);
        };
    }

    /** 回收过期租约；新一轮认领会拿到更大的 fencing token。 */
    public long reclaimExpired(String topic) {
        ensureOpen();
        validateTopic(topic);
        return evaluateLong("reclaim",
                keys(topic, "processing", "tokens", "scheduled", "completed"),
                properties.getBatchSize(), properties.getCompletedRetention().toMillis());
    }

    public boolean renewLease(String topic, String id, long fencingToken) {
        ensureOpen();
        validateTopic(topic);
        validateId(id);
        return evaluateLong("renew", keys(topic, "processing", "tokens"),
                id, Long.toString(fencingToken), properties.getLease().toMillis()) == 1L;
    }

    /** 只有尚未认领的 scheduled 消息可取消，已在执行中的业务不能被伪取消。 */
    public boolean cancelScheduled(String topic, String id) {
        ensureOpen();
        validateTopic(topic);
        validateId(id);
        return evaluateLong("cancel", keys(topic, "scheduled", "payload", "attempts"), id) == 1L;
    }

    /** 人工重放：将同一个稳定消息 ID 从 DLQ 原子移回 scheduled。 */
    public boolean replayDeadLetter(String topic, String id, Duration delay) {
        ensureOpen();
        validateTopic(topic);
        validateId(id);
        long delayMillis = positiveOrZero(delay, "delay");
        return evaluateLong("replay", keys(topic, "dlq", "dlq-meta", "scheduled", "payload", "attempts"),
                id, delayMillis) == 1L;
    }

    public String deadLetterPayload(String topic, String id) {
        ensureOpen();
        validateTopic(topic);
        validateId(id);
        RMap<String, String> dlq = redisson.getMap(key(topic, "dlq"), StringCodec.INSTANCE);
        return dlq.get(id);
    }

    public QueueStatus status(String topic) {
        ensureOpen();
        validateTopic(topic);
        List<Object> result = evaluateList("status",
                keys(topic, "scheduled", "processing", "dlq", "completed"));
        if (result.size() != 4) {
            throw new IllegalStateException("Invalid delay queue V2 status response");
        }
        return new QueueStatus(toLong(result.get(0)), toLong(result.get(1)),
                toLong(result.get(2)), toLong(result.get(3)));
    }

    /**
     * 启动一个主题的有界串行消费者。多实例可以分别调用 start(topic, task)。
     * 每个实例最多并行 workerCount 个主题，同一主题不创建重复轮询器。
     */
    public synchronized void start(String topic, ConsumerTask task) {
        ensureOpen();
        validateTopic(topic);
        Objects.requireNonNull(task, "task");
        if (!topic.equals(task.topic())) {
            throw new IllegalArgumentException("Delay queue V2 task topic does not match requested topic");
        }
        if (pollingTopics.containsKey(topic)) {
            return;
        }
        if (pollingTopics.size() >= properties.getWorkerCount()) {
            throw new IllegalStateException("Delay queue V2 worker topic limit reached");
        }
        long period = properties.getPollInterval().toMillis();
        ScheduledFuture<?> future = poller.scheduleWithFixedDelay(
                () -> pollSafely(topic, task), 0, period, TimeUnit.MILLISECONDS);
        pollingTopics.put(topic, future);
    }

    public synchronized void stop(String topic) {
        ScheduledFuture<?> future = pollingTopics.remove(topic);
        if (future != null) {
            future.cancel(false);
        }
    }

    /** 手工执行一次恢复与认领，便于运维验证和测试。 */
    public int pollOnce(String topic, ConsumerTask task) {
        ensureOpen();
        validateTopic(topic);
        Objects.requireNonNull(task, "task");
        if (!topic.equals(task.topic())) {
            throw new IllegalArgumentException("Delay queue V2 task topic mismatch");
        }
        reclaimExpired(topic);
        List<ClaimedMessage> claimed = claim(topic);
        for (ClaimedMessage message : claimed) {
            String key = topic + "/" + message.id();
            active.put(key, new LeasedMessage(topic, message));
            try {
                task.execute(message.payload());
                if (!acknowledge(topic, message.id(), message.fencingToken())) {
                    log.error("Delay queue V2 ACK lost fencing token, topic={}, id={}", topic, message.id());
                }
            } catch (Exception exception) {
                log.warn("Delay queue V2 task failed, topic={}, id={}, attempts={}",
                        topic, message.id(), message.attempts(), exception);
                FailureOutcome outcome = fail(topic, message);
                log.info("Delay queue V2 failure outcome={}, topic={}, id={}", outcome, topic, message.id());
            } finally {
                active.remove(key);
            }
        }
        return claimed.size();
    }

    private void pollSafely(String topic, ConsumerTask task) {
        try {
            pollOnce(topic, task);
        } catch (Exception exception) {
            log.error("Delay queue V2 poll failed, topic={}", topic, exception);
        }
    }

    private void renewSafely() {
        for (LeasedMessage lease : active.values()) {
            try {
                boolean renewed = renewLease(lease.topic(), lease.message().id(), lease.message().fencingToken());
                if (!renewed) {
                    log.warn("Delay queue V2 lease no longer owned, topic={}, id={}",
                            lease.topic(), lease.message().id());
                }
            } catch (Exception exception) {
                log.error("Delay queue V2 lease renewal failed, topic={}, id={}",
                        lease.topic(), lease.message().id(), exception);
            }
        }
    }

    private long backoffMillis(long attempt) {
        int exponent = (int) Math.min(20, Math.max(0, attempt - 1));
        long factor = 1L << exponent;
        long base = properties.getRetryInitial().toMillis();
        long max = properties.getRetryMax().toMillis();
        return base > max / factor ? max : base * factor;
    }

    private List<Object> keys(String topic, String... suffixes) {
        List<Object> keys = new ArrayList<>(suffixes.length);
        for (String suffix : suffixes) {
            keys.add(key(topic, suffix));
        }
        return keys;
    }

    private static String key(String topic, String suffix) {
        return "peach:dq2:{" + topic + "}:" + suffix;
    }

    private static void validateTopic(String topic) {
        if (topic == null || !topic.matches(TOPIC_PATTERN)) {
            throw new IllegalArgumentException("Invalid delay queue V2 topic");
        }
    }

    private static void validateId(String id) {
        if (id == null || !id.matches(ID_PATTERN)) {
            throw new IllegalArgumentException("Invalid delay queue V2 stable message ID");
        }
    }

    private static long positiveOrZero(Duration duration, String name) {
        if (duration == null || duration.isNegative()) {
            throw new IllegalArgumentException(name + " must be nonnegative");
        }
        return duration.toMillis();
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("Delay queue V2 has been closed");
        }
    }

    private long evaluateLong(String name, List<Object> keys, Object... args) {
        Long result = redisScripts.eval(RScript.Mode.READ_WRITE, scripts.get(name),
                RScript.ReturnType.INTEGER, keys, args);
        if (result == null) {
            throw new IllegalStateException("Redis Lua returned null, script=" + name);
        }
        return result;
    }

    private List<Object> evaluateList(String name, List<Object> keys, Object... args) {
        List<Object> result = redisScripts.eval(RScript.Mode.READ_WRITE, scripts.get(name),
                RScript.ReturnType.MULTI, keys, args);
        return result == null ? List.of() : result;
    }

    private static long toLong(Object number) {
        return number instanceof Number n ? n.longValue() : Long.parseLong(String.valueOf(number));
    }

    private static String script(String resource) {
        try (InputStream in = new ClassPathResource(SCRIPT_ROOT + resource).getInputStream()) {
            return StreamUtils.copyToString(in, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Missing delay queue V2 Lua script: " + resource, exception);
        }
    }

    @Override
    public void close() {
        closed = true;
        for (ScheduledFuture<?> future : pollingTopics.values()) {
            future.cancel(false);
        }
        pollingTopics.clear();
        poller.shutdown();
        try {
            if (!poller.awaitTermination(5, TimeUnit.SECONDS)) {
                poller.shutdownNow();
            }
        } catch (InterruptedException exception) {
            poller.shutdownNow();
            Thread.currentThread().interrupt();
        }
        heartbeat.shutdownNow();
        active.clear();
    }
}
