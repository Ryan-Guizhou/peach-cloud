package com.peach.captcha.service.impl.cache;

import com.peach.captcha.service.CaptchaCacheService;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Objects;

/**
 * 每个应用上下文独立的有界内存验证码缓存；按访问/写入惰性移除过期记录，
 * 原子计数与原子消费均在短同步临界区内完成，不创建私有清理线程。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:55
 */
public class MemoryCaptchaCacheService implements CaptchaCacheService {

    private static final int DEFAULT_MAX_ENTRIES = 1000;
    private static final int MAX_VALUE_LENGTH = 65536;

    private final Map<String, Entry> entries = new ConcurrentHashMap<>();
    private final int maxEntries;
    private final Clock clock;

    public MemoryCaptchaCacheService() {
        this(DEFAULT_MAX_ENTRIES);
    }

    public MemoryCaptchaCacheService(int maxEntries) {
        this(maxEntries, Clock.systemUTC());
    }

    /** 允许注入固定时钟以验证过期行为。 */
    MemoryCaptchaCacheService(int maxEntries, Clock clock) {
        if (maxEntries < 1 || maxEntries > 1_000_000) {
            throw new IllegalArgumentException("Captcha memory maxEntries must be 1..1000000");
        }
        this.maxEntries = maxEntries;
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public synchronized void set(String key, String value, long expiresInSeconds) {
        validateWrite(key, value, expiresInSeconds);
        Instant now = clock.instant();
        if (!entries.containsKey(key)) {
            enforceCapacity(now);
        }
        entries.put(key, new Entry(value, now.plusSeconds(expiresInSeconds)));
    }

    @Override
    public boolean exists(String key) {
        return readCurrent(key) != null;
    }

    @Override
    public synchronized void delete(String key) {
        entries.remove(key);
    }

    @Override
    public String get(String key) {
        Entry current = readCurrent(key);
        return current == null ? null : current.value();
    }

    @Override
    public Long increment(String key, long val) {
        return incrementAndExpire(key, val, 60L);
    }

    @Override
    public synchronized Long incrementAndExpire(String key, long val, long expiresInSeconds) {
        if (val <= 0L) {
            throw new IllegalArgumentException("Captcha increment must be positive");
        }
        String existing = get(key);
        long next = existing == null ? val : Math.addExact(Long.parseLong(existing), val);
        if (existing == null) {
            set(key, Long.toString(next), expiresInSeconds);
        } else {
            Entry current = entries.get(key);
            entries.put(key, new Entry(Long.toString(next), current.expiresAt()));
        }
        return next;
    }

    @Override
    public synchronized String getAndDelete(String key) {
        Entry current = readCurrent(key);
        entries.remove(key);
        return current == null ? null : current.value();
    }

    /** 清理过期键，只有内部显式调用和写入超限时才触发扫描。 */
    public synchronized void cleanExpired() {
        Instant now = clock.instant();
        entries.entrySet().removeIf(entry -> !entry.getValue().expiresAt().isAfter(now));
    }

    public synchronized void clear() {
        entries.clear();
    }

    private Entry readCurrent(String key) {
        if (key == null) {
            return null;
        }
        Entry current = entries.get(key);
        if (current != null && !current.expiresAt().isAfter(clock.instant())) {
            entries.remove(key, current);
            return null;
        }
        return current;
    }

    private void enforceCapacity(Instant now) {
        if (entries.size() < maxEntries) {
            return;
        }
        entries.entrySet().removeIf(entry -> !entry.getValue().expiresAt().isAfter(now));
        if (entries.size() >= maxEntries) {
            throw new IllegalStateException("Captcha memory cache capacity reached");
        }
    }

    private static void validateWrite(String key, String value, long ttl) {
        if (key == null || key.isBlank() || key.length() > 512 || value == null
                || value.length() > MAX_VALUE_LENGTH || ttl < 1L || ttl > 604800L) {
            throw new IllegalArgumentException("Captcha cache key, value or TTL is invalid");
        }
    }

    private record Entry(String value, Instant expiresAt) {
    }
}
