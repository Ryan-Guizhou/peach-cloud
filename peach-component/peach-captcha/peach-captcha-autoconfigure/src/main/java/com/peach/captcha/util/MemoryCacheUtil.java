package com.peach.captcha.util;

import com.peach.captcha.service.impl.cache.MemoryCaptchaCacheService;

/**
 * 历史静态内存验证码缓存兼容入口。
 *
 * <p>Spring 自动配置不再经过这个全局入口；各上下文拥有独立的
 * {@link MemoryCaptchaCacheService}。旧静态 API 只保留一个有界实例，
 * 不再创建 ScheduledExecutor 或 JVM shutdown hook。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 18:11
 */
public final class MemoryCacheUtil {

    private static volatile MemoryCaptchaCacheService cache = new MemoryCaptchaCacheService();

    private MemoryCacheUtil() {
        throw new IllegalStateException("Utility class");
    }

    /** 重建旧兼容实例；定时清理参数只保留签名，不启动后台任务。 */
    public static synchronized void init(int cacheMaxNumber, long second) {
        if (second < 0) {
            throw new IllegalArgumentException("Captcha cleaner interval must not be negative");
        }
        cache = new MemoryCaptchaCacheService(cacheMaxNumber);
    }

    public static void set(String key, Object value, long expiresInSeconds) {
        cache.set(key, String.valueOf(value), expiresInSeconds);
    }

    public static String get(String key) {
        return cache.get(key);
    }

    public static long incrementAndExpire(String key, long increment, long expiresInSeconds) {
        return cache.incrementAndExpire(key, increment, expiresInSeconds);
    }

    public static String getAndDelete(String key) {
        return cache.getAndDelete(key);
    }

    public static void refresh() {
        cache.cleanExpired();
    }

    public static boolean exists(String key) {
        return cache.exists(key);
    }

    public static void remove(String key) {
        cache.delete(key);
    }

    public static void clear() {
        cache.clear();
    }
}
