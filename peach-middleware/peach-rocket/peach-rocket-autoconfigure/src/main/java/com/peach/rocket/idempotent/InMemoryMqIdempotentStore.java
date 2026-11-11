package com.peach.rocket.idempotent;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * InMemoryMQ幂等存储。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/26
 */
public class InMemoryMqIdempotentStore implements MqIdempotentStore {

    private static final int CLEANUP_INTERVAL = 1024;

    private final Map<String, Record> records = new ConcurrentHashMap<String, Record>();

    private final AtomicInteger attempts = new AtomicInteger();

    @Override
    public boolean tryStart(MqIdempotentContext context) {
        if (attempts.incrementAndGet() % CLEANUP_INTERVAL == 0) {
            cleanupExpired();
        }
        String key = storageKey(context);
        Instant now = Instant.now();
        Record newRecord = new Record(Status.PROCESSING, now.plus(context.expire()));
        AtomicBoolean acquired = new AtomicBoolean(false);
        records.compute(key, (ignored, old) -> {
            if (old == null || !old.getExpiresAt().isAfter(now)) {
                acquired.set(true);
                return newRecord;
            }
            return old;
        });
        return acquired.get();
    }

    @Override
    public void markSuccess(MqIdempotentContext context) {
        String key = storageKey(context);
        records.computeIfPresent(key, (k, old) -> new Record(Status.SUCCESS, old.getExpiresAt()));
    }

    @Override
    public void markFailed(MqIdempotentContext context) {
        records.remove(storageKey(context));
    }

    @Override
    public boolean isSuccess(MqIdempotentContext context) {
        Record storedRecord = records.get(storageKey(context));
        return storedRecord != null && storedRecord.getStatus() == Status.SUCCESS && storedRecord.getExpiresAt().isAfter(Instant.now());
    }

    private void cleanupExpired() {
        Instant now = Instant.now();
        for (Map.Entry<String, Record> entry : records.entrySet()) {
            if (entry.getValue().getExpiresAt().isBefore(now)) {
                records.remove(entry.getKey());
            }
        }
    }

    private String storageKey(MqIdempotentContext context) {
        return context.consumerGroup() + ':' + context.idempotentKey();
    }

    /**
     * Status枚举。
     *
     * @Author Mr Shu
     * @Version 1.0.0
     * @CreateTime 2026/3/20 16:58
     */

    private enum Status {
        PROCESSING,
        SUCCESS
    }

    /**
     * 记录。
     *
     * @Author Mr Shu
     * @Version 1.0.0
     * @CreateTime 2026/3/20 16:58
     */
    private static class Record {
        private final Status status;
        private final Instant expiresAt;
        Record(Status status, Instant expiresAt) {
            this.status = status;
            this.expiresAt = expiresAt;
        }
        Status getStatus() { return status; }
        Instant getExpiresAt() { return expiresAt; }
    }
}
