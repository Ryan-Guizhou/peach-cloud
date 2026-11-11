package com.peach.email.idempotency;

import com.peach.email.core.SendResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 单进程原子邮件幂等存储，有限容量并按 TTL 惰性清理。
 *
 * <p>状态包含发送中、成功、投递未知；只允许认领令牌拥有者变更记录。
 * 本存储不是跨实例持久化保障，生产水平扩容须替换为共享 ClaimingIdempotencyStore。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/9 15:19
 */
@Slf4j
public class SimpleIdempotencyStore implements ClaimingIdempotencyStore {

    private static final int DEFAULT_MAX_ENTRIES = 10_000;

    private static final Duration DEFAULT_RETENTION = Duration.ofHours(24);

    private final Map<String, Entry> store = new ConcurrentHashMap<>();
    private final Duration retention;
    private final int maximumEntries;
    private final Clock clock;
    private int claimCount;

    public SimpleIdempotencyStore() {
        this(DEFAULT_RETENTION, DEFAULT_MAX_ENTRIES, Clock.systemUTC());
    }

    public SimpleIdempotencyStore(Duration retention, int maximumEntries) {
        this(retention, maximumEntries, Clock.systemUTC());
    }

    /**
     * 支持可控时钟的构造器，便于无 Sleep 测试过期与重领。
     */
    public SimpleIdempotencyStore(Duration retention, int maximumEntries, Clock clock) {
        if (retention == null || retention.isZero() || retention.isNegative()
                || retention.compareTo(Duration.ofDays(365)) > 0 || maximumEntries < 1) {
            throw new IllegalArgumentException("Email idempotency retention/capacity is invalid");
        }
        this.retention = retention;
        this.maximumEntries = maximumEntries;
        this.clock = Objects.requireNonNull(clock, "clock");
        log.info("Init SimpleIdempotencyStore, maximumEntries={}", maximumEntries);
    }

    @Override
    public boolean exists(String key) {
        if (StringUtils.isBlank(key)) {
            return false;
        }
        Entry entry = store.get(key);
        if (entry == null) {
            return false;
        }
        if (!entry.expiresAt().isAfter(clock.instant())) {
            store.remove(key, entry);
            return false;
        }
        return entry.status() == Status.SENT;
    }

    @Override
    public synchronized String tryClaim(String key) {
        if (StringUtils.isBlank(key)) {
            throw new IllegalArgumentException("Email idempotency key must not be blank");
        }
        Instant now = clock.instant();
        Entry current = store.get(key);
        if (current != null && current.expiresAt().isAfter(now)) {
            return null;
        }
        if (++claimCount % 256 == 0 || store.size() >= maximumEntries) {
            store.entrySet().removeIf(entry -> !entry.getValue().expiresAt().isAfter(now));
        }
        if (store.size() >= maximumEntries && !store.containsKey(key)) {
            throw new IllegalStateException("Email idempotency store capacity reached");
        }
        String claimToken = UUID.randomUUID().toString();
        store.put(key, new Entry(Status.IN_FLIGHT, claimToken, null, now.plus(retention)));
        return claimToken;
    }

    @Override
    public synchronized void markSent(String key, String claimToken, SendResult result) {
        if (result == null || !result.isSuccess()) {
            throw new IllegalArgumentException("Only successful results can complete email claims");
        }
        requireCurrentClaim(key, claimToken);
        store.put(key, new Entry(Status.SENT, claimToken, result, clock.instant().plus(retention)));
    }

    @Override
    public synchronized void markUnknown(String key, String claimToken) {
        requireCurrentClaim(key, claimToken);
        store.put(key, new Entry(Status.UNKNOWN, claimToken, null, clock.instant().plus(retention)));
    }

    @Override
    public synchronized void releaseClaim(String key, String claimToken) {
        Entry current = store.get(key);
        if (current != null && current.status() == Status.IN_FLIGHT
                && Objects.equals(current.claimToken(), claimToken)) {
            store.remove(key, current);
        }
    }

    @Override
    public synchronized void storeSendResult(String key, SendResult result) {
        if (StringUtils.isBlank(key) || result == null || !result.isSuccess()) {
            return;
        }
        Instant now = clock.instant();
        Entry current = store.get(key);
        if (current != null && current.expiresAt().isAfter(now)) {
            if (current.status() != Status.SENT) {
                throw new IllegalStateException("Cannot overwrite an active email idempotency claim");
            }
            return;
        }
        if (store.size() >= maximumEntries) {
            store.entrySet().removeIf(entry -> !entry.getValue().expiresAt().isAfter(now));
        }
        if (store.size() >= maximumEntries) {
            throw new IllegalStateException("Email idempotency store capacity reached");
        }
        store.put(key, new Entry(Status.SENT, null, result, now.plus(retention)));
    }

    private void requireCurrentClaim(String key, String token) {
        Entry current = store.get(key);
        if (current == null || current.status() != Status.IN_FLIGHT
                || token == null || !token.equals(current.claimToken())
                || !current.expiresAt().isAfter(clock.instant())) {
            throw new IllegalStateException("Email idempotency claim expired or is not owned by caller");
        }
    }

    private enum Status {
        IN_FLIGHT, SENT, UNKNOWN
    }

    private record Entry(Status status, String claimToken, SendResult result, Instant expiresAt) {
    }
}
