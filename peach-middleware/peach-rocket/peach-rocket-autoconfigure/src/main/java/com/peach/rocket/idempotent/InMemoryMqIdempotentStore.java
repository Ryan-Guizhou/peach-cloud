package com.peach.rocket.idempotent;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.util.StringUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * 有界的单 JVM 消费幂等存储，支持 owner token 核对与过期认领。
 *
 * <p>仅用于本地开发和测试。业务生产需要跨实例持久化 Store；容量耗尽时明确拒绝
 * 新消息，不会清除尚在处理的幂等记录。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/12
 */
@Slf4j
public class InMemoryMqIdempotentStore implements MqIdempotentStore {
    @Override
    public boolean supportsLeaseRenewal() {
        return true;
    }

    private static final int DEFAULT_MAX_ENTRIES = 32768;
    private static final int CLEANUP_INTERVAL = 1024;

    private final Map<String, Record> records = new ConcurrentHashMap<>();
    private final Object capacityLock = new Object();
    private final int maxEntries;
    private int acquiredCount;

    public InMemoryMqIdempotentStore() {
        this(DEFAULT_MAX_ENTRIES);
    }

    /**
     * @param maxEntries 最大幂等记录数
     */
    public InMemoryMqIdempotentStore(int maxEntries) {
        if (maxEntries < 1) {
            throw new IllegalArgumentException("Idempotent maxEntries must be positive");
        }
        this.maxEntries = maxEntries;
    }

    @Override
    public MqIdempotentClaim claim(MqIdempotentContext context) {
        String key = storageKey(context);
        Instant now = Instant.now();
        synchronized (capacityLock) {
            Record existing = records.get(key);
            if (existing != null && existing.expiresAt().isAfter(now)) {
                return existing.status() == Status.SUCCESS
                        ? MqIdempotentClaim.succeeded() : MqIdempotentClaim.inProgress();
            }
            if (++acquiredCount % CLEANUP_INTERVAL == 0 || (existing == null && records.size() >= maxEntries)) {
                cleanupExpired(now);
            }
            if (existing == null && records.size() >= maxEntries) {
                log.warn("[mq-idempotent] in-memory capacity reached. maxEntries={} retainedEntries={}",
                        maxEntries, records.size());
                throw new IllegalStateException("In-memory MQ idempotent capacity reached; configure a durable store");
            }
            String token = UUID.randomUUID().toString();
            records.put(key, new Record(Status.PROCESSING, now.plus(context.expire()), token));
            return MqIdempotentClaim.acquired(token);
        }
    }

    @Override
    public boolean tryStart(MqIdempotentContext context) {
        synchronized (capacityLock) {
            MqIdempotentClaim claim = claim(context);
            if (claim.status() != MqIdempotentClaim.Status.ACQUIRED) return false;
            Record owned = records.get(storageKey(context));
            records.put(storageKey(context), new Record(owned.status(), owned.expiresAt(), null));
            return true;
        }
    }

    @Override
    public void markSuccess(MqIdempotentContext context, MqIdempotentClaim claim) {
        synchronized (capacityLock) {
            Record current = requireOwner(context, claim);
            records.put(storageKey(context), new Record(Status.SUCCESS, Instant.now().plus(context.expire()), null));
        }
    }

    @Override
    public void markFailed(MqIdempotentContext context, MqIdempotentClaim claim) {
        synchronized (capacityLock) {
            requireOwner(context, claim);
            records.remove(storageKey(context));
        }
    }

    @Override
    public boolean supportsOwnerFencing() {
        return true;
    }

    /**
     * 兼容历史直接调用；新消费链路必须使用携带 owner token 的重载。
     */
    @Override
    public void markSuccess(MqIdempotentContext context) {
        synchronized (capacityLock) {
            String key = storageKey(context);
            Record old = records.get(key);
            if (old != null && old.status() == Status.PROCESSING && old.ownerToken() == null) {
                records.put(key, new Record(Status.SUCCESS, old.expiresAt(), old.ownerToken()));
            }
        }
    }

    /**
     * 兼容历史直接调用；生产环境禁止使用无认领身份的状态更新。
     */
    @Override
    public void markFailed(MqIdempotentContext context) {
        synchronized (capacityLock) {
            Record old = records.get(storageKey(context));
            if (old != null && old.status() == Status.PROCESSING && old.ownerToken() == null) {
                records.remove(storageKey(context));
            }
        }
    }

    @Override
    public boolean isSuccess(MqIdempotentContext context) {
        Record record = records.get(storageKey(context));
        return record != null && record.status() == Status.SUCCESS
                && record.expiresAt().isAfter(Instant.now());
    }

    @Override
    public boolean renew(MqIdempotentContext context, MqIdempotentClaim claim) {
        synchronized (capacityLock) {
            try {
                Record current = requireOwner(context, claim);
                records.put(storageKey(context), new Record(Status.PROCESSING,
                        Instant.now().plus(context.expire()), current.ownerToken()));
                return true;
            } catch (IllegalStateException stale) {
                return false;
            }
        }
    }

    private Record requireOwner(MqIdempotentContext context, MqIdempotentClaim claim) {
        if (claim == null || claim.status() != MqIdempotentClaim.Status.ACQUIRED
                || !StringUtils.hasText(claim.ownerToken())) {
            throw new IllegalArgumentException("MQ idempotent owner token is required");
        }
        Record current = records.get(storageKey(context));
        if (current == null || current.status() != Status.PROCESSING
                || !claim.ownerToken().equals(current.ownerToken()) || !current.expiresAt().isAfter(Instant.now())) {
            throw new IllegalStateException("Stale MQ idempotent owner cannot update the record");
        }
        return current;
    }

    @Override
    public int cleanupTerminal(int limit) {
        if (limit < 1) {
            throw new IllegalArgumentException("Cleanup limit must be positive");
        }
        synchronized (capacityLock) {
            int removed = 0;
            Instant now = Instant.now();
            for (var entry : records.entrySet()) {
                Record old = entry.getValue();
                if (old.status() == Status.SUCCESS && !old.expiresAt().isAfter(now)
                        && records.remove(entry.getKey(), old) && ++removed >= Math.min(500, limit)) break;
            }
            return removed;
        }
    }

    private void cleanupExpired(Instant now) {
        for (Map.Entry<String, Record> entry : records.entrySet()) {
            if (!entry.getValue().expiresAt().isAfter(now)) {
                records.remove(entry.getKey(), entry.getValue());
            }
        }
    }

    private String storageKey(MqIdempotentContext context) {
        return context.consumerGroup().length() + ":" + context.consumerGroup() + context.idempotentKey();
    }

    private enum Status {
        PROCESSING, SUCCESS
    }

    private record Record(Status status, Instant expiresAt, String ownerToken) {
    }
}
