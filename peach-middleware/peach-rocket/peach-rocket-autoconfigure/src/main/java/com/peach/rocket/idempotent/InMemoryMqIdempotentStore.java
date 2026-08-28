package com.peach.rocket.idempotent;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * InMemoryMQ幂等存储。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/26
 */
public class InMemoryMqIdempotentStore implements MqIdempotentStore {

    private final Map<String, Record> records = new ConcurrentHashMap<String, Record>();

    @Override
    public boolean tryStart(MqIdempotentContext context) {
        cleanupExpired();
        String key = storageKey(context);
        Record newRecord = new Record(Status.PROCESSING, Instant.now().plus(context.expire()));
        Record old = records.putIfAbsent(key, newRecord);
        if (old == null) {
            return true;
        }
        if (old.getExpiresAt().isBefore(Instant.now())) {
            records.put(key, newRecord);
            return true;
        }
        return false;
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
