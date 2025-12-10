package com.peach.sample.email.Idempotency;

import com.peach.email.Idempotency.IdempotencyStore;
import com.peach.email.core.SendResult;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;


/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/10 10:59
 * 带过期时间的幂等存储
 * 支持设置记录有效期，超过 TTL 自动失效
 */
@Slf4j
public class TTLIdempotencyStore implements IdempotencyStore {

    private final ConcurrentHashMap<String, Entry> store = new ConcurrentHashMap<>();

    private final long ttlMillis; // 记录有效期，单位毫秒

    private static final long DEFAULT_TTL_MILLIS = 1000 * 60 * 5;

    public TTLIdempotencyStore() {
        this(DEFAULT_TTL_MILLIS);
    }


    public TTLIdempotencyStore(long ttlMillis) {
        if (ttlMillis <= 0) {
            throw new IllegalArgumentException("ttlMillis must > 0");
        }
        this.ttlMillis = ttlMillis;
        log.info("Init TTLIdempotencyStore successful: ttlMillis={}", ttlMillis);
    }

    @Override
    public boolean exists(String key) {
        return Optional.ofNullable(key)
                .filter(k -> Objects.nonNull(k))
                .map(k -> {
                    Entry entry = store.get(k);
                    if (entry == null){
                        return false;
                    }
                    // 判断是否过期
                    if (Instant.now().toEpochMilli() - entry.timestamp > ttlMillis) {
                        store.remove(k);
                        return false;
                    }
                    return true;
                }).orElse(false);
    }

    @Override
    public void record(String key, SendResult result) {
        Optional.ofNullable(key)
                .filter(k -> Objects.nonNull(k))
                .flatMap(k -> Optional.ofNullable(result))
                .filter(SendResult::isSuccess) // 可选，只记录成功
                .ifPresent(r -> store.put(key, new Entry(r, Instant.now().toEpochMilli())));
    }

    /**
     * 存储实体，记录结果和时间戳
     */
    private static class Entry {

        private final SendResult result;

        private final long timestamp;

        public Entry(SendResult result, long timestamp) {
            this.result = result;
            this.timestamp = timestamp;
        }
    }
}
