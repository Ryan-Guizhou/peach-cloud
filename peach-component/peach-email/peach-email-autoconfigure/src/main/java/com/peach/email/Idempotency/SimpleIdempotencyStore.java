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
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 单进程原子邮件幂等存储，采用 Key 粒度状态转换与严格容量许可。
 *
 * <p>保留 IN_FLIGHT、SENT、UNKNOWN 的所有权语义；同键修改由
 * ConcurrentHashMap.compute 串行，不同业务键不争用一个 Java 对象监视器。
 * 本存储不是跨实例持久化保证，多实例必须使用共享 ClaimingIdempotencyStore。</p>
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
    /** 仅新键占用许可；原键过期重领复用已有许可，不会突破容量。 */
    private final Semaphore capacity;
    private final Duration retention;
    private final Clock clock;
    private final AtomicInteger claimCount = new AtomicInteger();

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
        this.capacity = new Semaphore(maximumEntries);
        this.clock = Objects.requireNonNull(clock, "clock");
        log.info("Init SimpleIdempotencyStore, maximumEntries={}", maximumEntries);
    }

    @Override
    public boolean exists(String key) {
        if (StringUtils.isBlank(key)) {
            return false;
        }
        Entry current = store.get(key);
        if (current == null) {
            return false;
        }
        if (!current.expiresAt().isAfter(clock.instant())) {
            removeExpired(key, current);
            return false;
        }
        return current.status() == Status.SENT;
    }

    @Override
    public String tryClaim(String key) {
        if (StringUtils.isBlank(key)) {
            throw new IllegalArgumentException("Email idempotency key must not be blank");
        }
        Instant now = clock.instant();
        if (claimCount.incrementAndGet() % 256 == 0 || capacity.availablePermits() == 0) {
            cleanExpired(now);
        }
        String token = UUID.randomUUID().toString();
        boolean[] claimed = new boolean[1];
        store.compute(key, (ignored, current) -> {
            if (current != null && current.expiresAt().isAfter(now)) {
                return current;
            }
            if (current == null && !capacity.tryAcquire()) {
                throw new IllegalStateException("Email idempotency store capacity reached");
            }
            claimed[0] = true;
            return new Entry(Status.IN_FLIGHT, token, null, now.plus(retention));
        });
        return claimed[0] ? token : null;
    }

    @Override
    public void markSent(String key, String claimToken, SendResult result) {
        if (result == null || !result.isSuccess()) {
            throw new IllegalArgumentException("Only successful results can complete email claims");
        }
        store.compute(key, (ignored, current) -> {
            requireCurrentClaim(current, claimToken);
            return new Entry(Status.SENT, claimToken, result, clock.instant().plus(retention));
        });
    }

    @Override
    public void markUnknown(String key, String claimToken) {
        store.compute(key, (ignored, current) -> {
            requireCurrentClaim(current, claimToken);
            return new Entry(Status.UNKNOWN, claimToken, null, clock.instant().plus(retention));
        });
    }

    @Override
    public void releaseClaim(String key, String claimToken) {
        if (key == null) {
            return;
        }
        boolean[] released = new boolean[1];
        store.computeIfPresent(key, (ignored, current) -> {
            if (current.status() == Status.IN_FLIGHT && Objects.equals(current.claimToken(), claimToken)) {
                released[0] = true;
                return null;
            }
            return current;
        });
        if (released[0]) {
            capacity.release();
        }
    }

    @Override
    public void storeSendResult(String key, SendResult result) {
        if (StringUtils.isBlank(key) || result == null || !result.isSuccess()) {
            return;
        }
        Instant now = clock.instant();
        if (capacity.availablePermits() == 0) {
            cleanExpired(now);
        }
        store.compute(key, (ignored, current) -> {
            if (current != null && current.expiresAt().isAfter(now)) {
                if (current.status() != Status.SENT) {
                    throw new IllegalStateException("Cannot overwrite an active email idempotency claim");
                }
                return current;
            }
            if (current == null && !capacity.tryAcquire()) {
                throw new IllegalStateException("Email idempotency store capacity reached");
            }
            return new Entry(Status.SENT, null, result, now.plus(retention));
        });
    }

    private void removeExpired(String key, Entry expected) {
        if (store.remove(key, expected)) {
            capacity.release();
        }
    }

    private void cleanExpired(Instant now) {
        // 不使用全局 monitor；CAS 删除后才释放许可，防止并发回收重复释放。
        store.forEach((key, entry) -> {
            if (!entry.expiresAt().isAfter(now)) {
                removeExpired(key, entry);
            }
        });
    }

    private void requireCurrentClaim(Entry current, String token) {
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
