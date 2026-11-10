package com.peach.redission.delayqueue.core;

import com.peach.redission.delayqueue.config.DelayQueueProperties;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RDelayedQueue;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 可靠延迟队列：在同一 Redis Cluster hash slot 内使用 Lua 原子状态转换。
 *
 * <p>仅有当前可靠实现。旧 RDelayedQueue 数据结构不兼容，必须在切换前排空。
 * 消费为 at-least-once；租约与 fencing token 只保护 Redis ACK，不替代
 * 外部数据库/接口的幂等、fencing 校验或事务提交。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/10
 */
@Slf4j
public final class RedisDelayQueue implements AutoCloseable {

    private static final String SCRIPT_ROOT = "lua/delayqueue/";
    private static final String TOPIC_PATTERN = "[a-zA-Z0-9._:-]{1,96}";
    private static final String ID_PATTERN = "[a-zA-Z0-9._:-]{1,128}";

    private final RedissonClient redisson;
    private final DelayQueueProperties properties;
    private final RScript redisScripts;
    private final Map<String, String> scripts;
    private final ScheduledThreadPoolExecutor poller;
    private final ExecutorService workers;
    private final Semaphore availableWorkers;
    private final ScheduledExecutorService heartbeat;
    private final Map<String, ScheduledFuture<?>> pollingTopics = new ConcurrentHashMap<>();
    private final Map<String, LeasedMessage> active = new ConcurrentHashMap<>();

    /**
     * 仅缓存当前进程已验证的物理主题，避免每条入队消息重复访问旧协议。
     * 旧实例必须在发布前停止，缓存并非跨实例写入栅栏。
     */
    private final Map<String, Boolean> inspectedLegacyTopics = new ConcurrentHashMap<>();
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

    /**
     * 旧 Redisson 队列的只读积压统计，用于发布切换与回滚评估。
     */
    public record LegacyQueueBacklog(long delayed, long ready, long processing, long deadLetters) {
        public boolean drained() {
            return delayed == 0L && ready == 0L && processing == 0L && deadLetters == 0L;
        }
    }

    public enum FailureOutcome {
        STALE_LEASE, RETRY_SCHEDULED, DEAD_LETTER
    }

    private record LeasedMessage(String topic, ClaimedMessage message) {
    }

    public RedisDelayQueue(RedissonClient redisson, DelayQueueProperties properties) {
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
            Thread thread = new Thread(task, "peach-delayqueue-poll-" + counter.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        });
        poller.setRemoveOnCancelPolicy(true);
        AtomicInteger workerCounter = new AtomicInteger(1);
        this.workers = Executors.newFixedThreadPool(properties.getWorkerCount(), task -> {
            Thread thread = new Thread(task, "peach-delayqueue-worker-" + workerCounter.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        });
        this.availableWorkers = new Semaphore(properties.getWorkerCount());
        this.heartbeat = Executors.newSingleThreadScheduledExecutor(task -> {
            Thread thread = new Thread(task, "peach-delayqueue-heartbeat");
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
        inspectLegacyOnce(topic);
        Objects.requireNonNull(payload, "payload");
        if (payload.getBytes(StandardCharsets.UTF_8).length > properties.getMaxPayloadBytes()) {
            throw new IllegalArgumentException("Delay queue payload exceeds configured maxPayloadBytes");
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
        return claim(topic, properties.getBatchSize());
    }

    /**
     * 根据空闲执行槽位限制原子认领数量，避免消息被认领后在本地等待工作线程。
     */
    private List<ClaimedMessage> claim(String topic, int limit) {
        ensureOpen();
        validateTopic(topic);
        inspectLegacyOnce(topic);
        if (limit < 1 || limit > properties.getBatchSize()) {
            throw new IllegalArgumentException("Invalid delay queue claim limit");
        }
        List<Object> flat = evaluateList("claim",
                keys(topic, "scheduled", "processing", "tokens", "attempts", "payload", "fencing"),
                properties.getLease().toMillis(), limit);
        if (flat.size() % 4 != 0) {
            throw new IllegalStateException("Invalid Redis delay queue claim response");
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
            default -> throw new IllegalStateException("Invalid delay queue fail result: " + result);
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
            throw new IllegalStateException("Invalid delay queue status response");
        }
        return new QueueStatus(toLong(result.get(0)), toLong(result.get(1)),
                toLong(result.get(2)), toLong(result.get(3)));
    }

    /**
     * 只读检查旧 Redisson 的 delayed/ready/processing/dead-letter 结构。
     * 不删除任何数据，也不修改当前 Lua 可靠队列的 key。
     * 这是发布前快照，不是原子禁止旧生产者继续写入的分布式栅栏。
     */
    public LegacyQueueBacklog legacyBacklog(String partitionTopic) {
        ensureOpen();
        validateTopic(partitionTopic);
        RBlockingQueue<String> ready = redisson.getBlockingQueue(partitionTopic);
        RDelayedQueue<String> delayed = redisson.getDelayedQueue(ready);
        // 先检查 delayed 后检查 ready，减少过期转移落在计数间的漏检窗口。
        long delayedCount = delayed.size();
        long readyCount = ready.size();
        long processingCount = redisson.getMap(partitionTopic + ":processing").size();
        long deadLetters = redisson.getMap(partitionTopic + "-dead-letter").size();
        return new LegacyQueueBacklog(delayedCount, readyCount, processingCount, deadLetters);
    }

    /**
     * 旧协议仍有未完成任务时拒绝切换；供部署前主动调用。
     */
    public void assertLegacyDrained(String partitionTopic) {
        LegacyQueueBacklog backlog = legacyBacklog(partitionTopic);
        if (!backlog.drained()) {
            throw new IllegalStateException("Historical delay queue not drained for topic="
                    + partitionTopic + ", ready=" + backlog.ready() + ", delayed=" + backlog.delayed()
                    + ", processing=" + backlog.processing() + ", deadLetters=" + backlog.deadLetters());
        }
    }

    private void inspectLegacyOnce(String topic) {
        inspectedLegacyTopics.computeIfAbsent(topic, name -> {
            assertLegacyDrained(name);
            return Boolean.TRUE;
        });
    }

    /**
     * 注册主题的有界轮询任务。认领和业务执行使用独立线程池；
     * 多主题共享 workerCount 个实际执行业务的线程，同一主题只注册一次。
     */
    public synchronized void start(String topic, ConsumerTask task) {
        ensureOpen();
        validateTopic(topic);
        Objects.requireNonNull(task, "task");
        if (!topic.equals(task.topic())) {
            throw new IllegalArgumentException("Delay queue task topic does not match requested topic");
        }
        if (pollingTopics.containsKey(topic)) {
            return;
        }
        // 对直接使用 RedisDelayQueue 的调用方也执行一次真实预检查。
        assertLegacyDrained(topic);
        inspectedLegacyTopics.put(topic, Boolean.TRUE);
        if (pollingTopics.size() >= properties.getMaxTopicSubscriptions()) {
            throw new IllegalStateException("Delay queue topic subscription limit reached");
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

    /**
     * 手工执行一个同步批次（供运维验证及现有调用方使用）。
     * 批次全部任务立即加入心跳集合，避免串行回调导致后续任务租约过期。
     */
    public int pollOnce(String topic, ConsumerTask task) {
        ensureOpen();
        validateTask(topic, task);
        reclaimExpired(topic);
        List<ClaimedMessage> claimed = claim(topic);
        List<LeasedMessage> leases = new ArrayList<>(claimed.size());
        for (ClaimedMessage message : claimed) {
            LeasedMessage lease = new LeasedMessage(topic, message);
            active.put(activeKey(lease), lease);
            leases.add(lease);
        }
        int executed = 0;
        try {
            for (LeasedMessage lease : leases) {
                executed++;
                processClaimed(task, lease);
            }
        } finally {
            // 业务回调或 Redis 异常中断同步批次时，未执行的租约应交由 Redis 回收。
            for (int i = executed; i < leases.size(); i++) {
                LeasedMessage unprocessed = leases.get(i);
                active.remove(activeKey(unprocessed), unprocessed);
            }
        }
        return claimed.size();
    }

    /**
     * 定时轮询仅在有实际执行槽位时认领，不允许已获取租约的消息排在无界本地队列中。
     * 失败时租约仍留在 Redis，由原有 reclaim 脚本恢复。
     */
    private void dispatchOnce(String topic, ConsumerTask task) {
        ensureOpen();
        validateTask(topic, task);
        reclaimExpired(topic);
        int limit = Math.min(properties.getBatchSize(), availableWorkers.availablePermits());
        if (limit == 0 || !availableWorkers.tryAcquire(limit)) {
            return;
        }
        int unused = limit;
        try {
            List<ClaimedMessage> claimed = claim(topic, limit);
            for (ClaimedMessage message : claimed) {
                LeasedMessage lease = new LeasedMessage(topic, message);
                String key = activeKey(lease);
                active.put(key, lease);
                try {
                    workers.execute(() -> {
                        try {
                            processClaimed(task, lease);
                        } finally {
                            availableWorkers.release();
                        }
                    });
                    unused--;
                } catch (RejectedExecutionException exception) {
                    active.remove(key, lease);
                    log.warn("Delay queue worker rejected an already claimed message; lease recovery required, topic={}, id={}",
                            topic, message.id(), exception);
                }
            }
        } finally {
            availableWorkers.release(unused);
        }
    }

    /**
     * 只清理本次租约的本地记录，避免过期持有者覆盖新认领者的心跳状态。
     */
    private void processClaimed(ConsumerTask task, LeasedMessage lease) {
        ClaimedMessage message = lease.message();
        String topic = lease.topic();
        try {
            task.execute(message.payload());
            if (!acknowledge(topic, message.id(), message.fencingToken())) {
                log.error("Delay queue ACK rejected; check business idempotency, topic={}, id={}",
                        topic, message.id());
            }
        } catch (Exception exception) {
            log.warn("Delay queue task failed, topic={}, id={}, attempts={}",
                    topic, message.id(), message.attempts(), exception);
            FailureOutcome outcome = fail(topic, message);
            log.info("Delay queue failure outcome={}, topic={}, id={}", outcome, topic, message.id());
        } finally {
            active.remove(activeKey(lease), lease);
        }
    }

    private static void validateTask(String topic, ConsumerTask task) {
        Objects.requireNonNull(task, "task");
        if (!topic.equals(task.topic())) {
            throw new IllegalArgumentException("Delay queue task topic mismatch");
        }
    }

    private static String activeKey(LeasedMessage lease) {
        return lease.topic() + "/" + lease.message().id();
    }

    private void pollSafely(String topic, ConsumerTask task) {
        try {
            dispatchOnce(topic, task);
        } catch (Exception exception) {
            if (!closed) {
                log.error("Delay queue poll failed, topic={}", topic, exception);
            }
        }
    }

    private void renewSafely() {
        for (LeasedMessage lease : active.values()) {
            try {
                boolean renewed = renewLease(lease.topic(), lease.message().id(), lease.message().fencingToken());
                if (!renewed) {
                    log.warn("Delay queue lease no longer owned, topic={}, id={}",
                            lease.topic(), lease.message().id());
                }
            } catch (Exception exception) {
                log.error("Delay queue lease renewal failed, topic={}, id={}",
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
            throw new IllegalArgumentException("Invalid delay queue topic");
        }
    }

    private static void validateId(String id) {
        if (id == null || !id.matches(ID_PATTERN)) {
            throw new IllegalArgumentException("Invalid delay queue stable message ID");
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
            throw new IllegalStateException("Delay queue has been closed");
        }
    }

    private long evaluateLong(String name, List<Object> keys, Object... args) {
        Long result = redisScripts.eval(RScript.Mode.READ_WRITE, scripts.get(name),
                RScript.ReturnType.INTEGER, keys, stringifyArgs(args));
        if (result == null) {
            throw new IllegalStateException("Redis Lua returned null, script=" + name);
        }
        return result;
    }

    private List<Object> evaluateList(String name, List<Object> keys, Object... args) {
        List<Object> result = redisScripts.eval(RScript.Mode.READ_WRITE, scripts.get(name),
                RScript.ReturnType.MULTI, keys, stringifyArgs(args));
        return result == null ? List.of() : result;
    }

    private static Object[] stringifyArgs(Object[] args) {
        Object[] strings = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            strings[i] = String.valueOf(args[i]);
        }
        return strings;
    }

    private static long toLong(Object number) {
        return number instanceof Number n ? n.longValue() : Long.parseLong(String.valueOf(number));
    }

    private static String script(String resource) {
        try (InputStream in = new ClassPathResource(SCRIPT_ROOT + resource).getInputStream()) {
            return StreamUtils.copyToString(in, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Missing delay queue Lua script: " + resource, exception);
        }
    }

    @Override
    public void close() {
        closed = true;
        for (ScheduledFuture<?> future : pollingTopics.values()) {
            future.cancel(false);
        }
        pollingTopics.clear();
        inspectedLegacyTopics.clear();
        poller.shutdownNow();
        workers.shutdown();
        try {
            if (!workers.awaitTermination(5, TimeUnit.SECONDS)) {
                log.warn("Delay queue worker shutdown timed out; unfinished leases remain reclaimable");
                workers.shutdownNow();
            }
        } catch (InterruptedException exception) {
            workers.shutdownNow();
            Thread.currentThread().interrupt();
        } finally {
            heartbeat.shutdownNow();
            active.clear();
        }
    }
}
