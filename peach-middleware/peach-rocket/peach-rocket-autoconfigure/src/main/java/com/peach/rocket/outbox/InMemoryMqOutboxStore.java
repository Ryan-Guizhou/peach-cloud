package com.peach.rocket.outbox;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.util.StringUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * 单 JVM 开发环境 Outbox，提供有限容量、租约认领和终态惰性回收。
 *
 * <p>仅是进程内 at-least-once 实现，进程退出后所有消息丢失。生产业务必须配置
 * 具备跨实例事务和认领持久性的 MqOutboxStore；容量耗尽时拒绝新消息而不丢弃未发送的消息。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/26
 */
@Slf4j
public class InMemoryMqOutboxStore implements MqOutboxStore {

    private static final int DEFAULT_MAX_ENTRIES = 1024;
    private static final int DEFAULT_MAX_PAYLOAD_BYTES = 65536;
    private static final int MAX_BATCH_SIZE = 500;
    private static final int MAX_ATTEMPTS = 20;
    private static final Duration DEFAULT_RETENTION = Duration.ofHours(24);
    private static final Duration DEFAULT_LEASE = Duration.ofMinutes(5);

    private final Map<String, Entry> events = new ConcurrentHashMap<>();
    private final Object saveLock = new Object();
    private final int maxEntries;
    private final int maxPayloadBytes;
    private final Duration retention;
    private final Duration lease;
    private final Clock clock;

    public InMemoryMqOutboxStore() {
        this(DEFAULT_MAX_ENTRIES, DEFAULT_MAX_PAYLOAD_BYTES, DEFAULT_RETENTION, DEFAULT_LEASE);
    }

    public InMemoryMqOutboxStore(int maxEntries, int maxPayloadBytes, Duration retention, Duration lease) {
        this(maxEntries, maxPayloadBytes, retention, lease, Clock.systemUTC());
    }

    /**
     * 注入时钟便于确定性验证过期、重试及租约保护。
     */
    InMemoryMqOutboxStore(int maxEntries, int maxPayloadBytes, Duration retention, Duration lease, Clock clock) {
        if (maxEntries < 1 || maxPayloadBytes < 1 || retention == null || retention.isNegative()
                || retention.isZero() || lease == null || lease.isNegative() || lease.isZero()) {
            throw new IllegalArgumentException("Invalid in-memory Outbox capacity or durations");
        }
        this.maxEntries = maxEntries;
        this.maxPayloadBytes = maxPayloadBytes;
        this.retention = retention;
        this.lease = lease;
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public void save(MqOutboxEvent event) {
        Objects.requireNonNull(event, "event");
        if (!StringUtils.hasText(event.messageId())
                || event.body() == null || event.body().length > maxPayloadBytes) {
            throw new IllegalArgumentException("Invalid in-memory Outbox ID or payload size");
        }
        Instant now = clock.instant();
        synchronized (saveLock) {
            if (events.containsKey(event.messageId())) {
                throw new IllegalStateException("Duplicate in-memory Outbox message ID");
            }
            if (events.size() >= maxEntries) {
                evictExpiredTerminal(now);
            }
            if (events.size() >= maxEntries) {
                log.warn("[mq-outbox] in-memory capacity reached. maxEntries={} retainedEntries={}",
                        maxEntries, events.size());
                throw new IllegalStateException("In-memory Outbox capacity reached; configure a durable store");
            }
            events.put(event.messageId(), new Entry(event, null, null, now, now));
        }
    }

    /**
     * 兼容旧调用方的只读待处理查询，不占用租约。Dispatcher 使用 claimPending。
     */
    @Override
    public List<MqOutboxEvent> findPending(int batchSize) {
        if (batchSize <= 0) {
            throw new IllegalArgumentException("Outbox batch size must be positive");
        }
        Instant now = clock.instant();
        List<MqOutboxEvent> result = new ArrayList<>();
        for (Entry entry : events.values()) {
            if (ready(entry, now)) {
                result.add(entry.event());
                if (result.size() >= Math.min(batchSize, MAX_BATCH_SIZE)) {
                    break;
                }
            }
        }
        return result;
    }

    /**
     * 每一条消息执行一次原子状态转换，多个 Dispatcher 不会认领相同的活跃租约。
     */
    @Override
    public List<MqOutboxClaim> claimPending(int batchSize) {
        if (batchSize <= 0) {
            throw new IllegalArgumentException("Outbox batch size must be positive");
        }
        Instant now = clock.instant();
        List<MqOutboxClaim> claims = new ArrayList<>();
        for (String id : events.keySet()) {
            if (claims.size() >= Math.min(batchSize, MAX_BATCH_SIZE)) {
                break;
            }
            AtomicReference<MqOutboxClaim> claimed = new AtomicReference<>();
            events.computeIfPresent(id, (key, previous) -> {
                if (!ready(previous, now) && !expiredLease(previous, now)) {
                    return previous;
                }
                String token = UUID.randomUUID().toString();
                MqOutboxEvent sending = previous.event().withStatus(MqOutboxStatus.SENDING);
                claimed.set(new MqOutboxClaim(sending, token));
                return new Entry(sending, token, now, now, now);
            });
            if (claimed.get() != null) {
                claims.add(claimed.get());
            }
        }
        return claims;
    }

    /**
     * 兼容只按 ID 更新的历史入口；不可用于多个 Dispatcher 的可靠认领。
     */
    @Override
    public void markSent(String messageId) {
        Instant now = clock.instant();
        events.computeIfPresent(messageId, (key, previous) ->
                previous.event().status() == MqOutboxStatus.FAILED ? previous
                        : new Entry(previous.event().withStatus(MqOutboxStatus.SENT),
                        null, null, now, now));
    }

    @Override
    public void markFailed(String messageId) {
        Instant now = clock.instant();
        events.computeIfPresent(messageId, (key, previous) -> {
            if (previous.event().status() == MqOutboxStatus.SENT
                    || previous.event().status() == MqOutboxStatus.FAILED) {
                return previous;
            }
            MqOutboxEvent retry = previous.event().withRetryCount(previous.event().retryCount() + 1)
                    .withStatus(MqOutboxStatus.RETRY);
            return new Entry(retry, null, null, now, now);
        });
    }

    @Override
    public void markSent(MqOutboxClaim claim) {
        updateClaim(claim, true);
    }

    @Override
    public void markFailed(MqOutboxClaim claim) {
        updateClaim(claim, false);
    }

    private void updateClaim(MqOutboxClaim claim, boolean sent) {
        Objects.requireNonNull(claim, "claim");
        if (!StringUtils.hasText(claim.claimToken())) {
            throw new IllegalArgumentException("In-memory Outbox requires a claim token");
        }
        Instant now = clock.instant();
        AtomicBoolean updated = new AtomicBoolean();
        events.computeIfPresent(claim.event().messageId(), (id, previous) -> {
            if (previous.event().status() != MqOutboxStatus.SENDING
                    || !claim.claimToken().equals(previous.claimToken())
                    || expiredLease(previous, now)) {
                return previous;
            }
            updated.set(true);
            if (sent) {
                return new Entry(previous.event().withStatus(MqOutboxStatus.SENT),
                        null, null, now, now);
            }
            int attempts = previous.event().retryCount() + 1;
            MqOutboxStatus status = attempts >= MAX_ATTEMPTS ? MqOutboxStatus.FAILED : MqOutboxStatus.RETRY;
            long seconds = Math.min(300L, 1L << Math.min(20, attempts - 1));
            return new Entry(previous.event().withRetryCount(attempts).withStatus(status),
                    null, null, now, status == MqOutboxStatus.FAILED ? now : now.plusSeconds(seconds));
        });
        if (!updated.get()) {
            throw new IllegalStateException("In-memory Outbox claim is stale");
        }
    }

    @Override
    public boolean replay(String messageId) {
        // 只允许 FAILED -> RETRY，其他状态不改变。
        AtomicBoolean replayed = new AtomicBoolean();
        Instant now = clock.instant();
        events.computeIfPresent(messageId, (key, previous) -> {
            if (previous.event().status() != MqOutboxStatus.FAILED) {
                return previous;
            }
            replayed.set(true);
            return new Entry(previous.event().withRetryCount(0).withStatus(MqOutboxStatus.RETRY),
                    null, null, now, now);
        });
        return replayed.get();
    }

    private boolean ready(Entry entry, Instant now) {
        return (entry.event().status() == MqOutboxStatus.INIT
                || entry.event().status() == MqOutboxStatus.RETRY)
                && !entry.nextAttemptAt().isAfter(now);
    }

    private boolean expiredLease(Entry entry, Instant now) {
        return entry.event().status() == MqOutboxStatus.SENDING
                && entry.claimedAt() != null && !entry.claimedAt().plus(lease).isAfter(now);
    }

    private void evictExpiredTerminal(Instant now) {
        for (Map.Entry<String, Entry> item : events.entrySet()) {
            Entry entry = item.getValue();
            if ((entry.event().status() == MqOutboxStatus.SENT
                    || entry.event().status() == MqOutboxStatus.FAILED)
                    && !entry.updatedAt().plus(retention).isAfter(now)) {
                events.remove(item.getKey(), entry);
            }
        }
    }

    private record Entry(MqOutboxEvent event, String claimToken, Instant claimedAt,
                         Instant updatedAt, Instant nextAttemptAt) {
    }
}
