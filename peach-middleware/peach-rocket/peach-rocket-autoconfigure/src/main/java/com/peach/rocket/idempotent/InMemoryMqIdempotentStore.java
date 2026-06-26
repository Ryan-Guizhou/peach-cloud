package com.peach.rocket.idempotent;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于内存的 MQ 幂等存储。
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/26
 */
public class InMemoryMqIdempotentStore implements MqIdempotentStore {

    private final Map<String, Record> records = new ConcurrentHashMap<String, Record>();

    @Override
    public boolean tryStart(MqIdempotentContext context) {
        cleanupExpired();
        String key = storageKey(context);
        Record newRecord = new Record(Status.PROCESSING, Instant.now().plus(context.getExpire()));
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
        Record old = records.get(key);
        if (old != null) {
            records.put(key, new Record(Status.SUCCESS, old.getExpiresAt()));
        }
    }

    @Override
    public void markFailed(MqIdempotentContext context) {
        records.remove(storageKey(context));
    }

    @Override
    public boolean isSuccess(MqIdempotentContext context) {
        Record record = records.get(storageKey(context));
        return record != null && record.getStatus() == Status.SUCCESS && record.getExpiresAt().isAfter(Instant.now());
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
        return context.getConsumerGroup() + ':' + context.getIdempotentKey();
    }

    private enum Status {
        PROCESSING,
        SUCCESS
    }

    /**
     * 内存幂等记录。
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
