package com.peach.captcha.util;

import com.peach.common.util.StringUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 18:11
 */
@Slf4j
public final class MemoryCacheUtil {

    private MemoryCacheUtil(){
        throw new IllegalStateException("Utility class");
    }

    private static final Map<String,Object> MEMORY_CACHE = new ConcurrentHashMap<>();

    /**
     * 缓存有效期key后缀
     */
    private static final String KEY_SUFFIX = "_HoldTime";

    /**
     * 缓存最大个数
     */
    private static Integer CACHE_MAX_NUMBER = 1000;

    /**
     * key + key_suffix 2
     */
    private static final Integer TWO = 2;

    private static ScheduledExecutorService cacheCleaner;

    /**
     * 初始化
     * @param cacheMaxNumber 缓存最大个数
     * @param second 定时任务 秒执行清除过期缓存
     */
    public static void init(int cacheMaxNumber, long second) {
        CACHE_MAX_NUMBER = cacheMaxNumber;
        if (second > 0L) {
            cacheCleaner = new ScheduledThreadPoolExecutor(1, r -> new Thread(r, "thd-captcha-cache-clean"),
                    new ThreadPoolExecutor.CallerRunsPolicy());
            cacheCleaner.scheduleAtFixedRate(MemoryCacheUtil::refresh, 10, second, TimeUnit.SECONDS);

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                clear();
                shutdownCacheCleaner();
            }));
        }
    }

    private static void shutdownCacheCleaner() {
        if (cacheCleaner != null) {
            cacheCleaner.shutdownNow();
            cacheCleaner = null;
        }
    }


    public static void set(String key, Object value, long expiresInSeconds) {
        if (MEMORY_CACHE.size() > CACHE_MAX_NUMBER * TWO){
            refresh();
            if (MEMORY_CACHE.size() > CACHE_MAX_NUMBER * TWO){
                clear();
            }
        }
        MEMORY_CACHE.put(key, value);
        MEMORY_CACHE.put(key + KEY_SUFFIX, System.currentTimeMillis() + expiresInSeconds * 1000);
    }

    public static String get(String key) {
        Object o = MEMORY_CACHE.get(key);
        return o == null ? null : StringUtil.getStringValue(o);
    }

    /**
     * 定时刷新过期缓存,删除过期缓存
     */
    public static void refresh(){
        log.debug("Local cache refresh, clear expired data");
        for (String key : MEMORY_CACHE.keySet()) {
            exists(key);
        }
    }

    /**
     * 获取缓存
     * @param key 缓存key
     * @return 缓存结果
     */
    public static boolean exists(String key) {
        Long holdTime = (Long) MEMORY_CACHE.get(key + KEY_SUFFIX);
        if (holdTime == null || holdTime == 0L){
            return false;
        }
        if (holdTime < System.currentTimeMillis()){
            remove(key);
            return false;
        }
        return MEMORY_CACHE.containsKey(key);
    }


    /**
     * 删除缓存
     * @param key 缓存key
     */
    public static void remove(String key){
        log.info("MemoryCacheUtil has remove key:{}",key);
        MEMORY_CACHE.remove(key);
        log.info("MemoryCacheUtil has remove key:{}",key + KEY_SUFFIX);
        MEMORY_CACHE.remove(key + KEY_SUFFIX);
    }

    /**
     * 清空缓存
     */
    public static void clear(){
        log.info("MemoryCacheUtil has clear all keys");
        MEMORY_CACHE.clear();
    }
}
