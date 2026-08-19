package com.peach.redission.common;

import com.peach.common.util.StringUtil;
import org.redisson.api.RedissonClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/19 18:53
 */
public class RedissionDataHandle {

    private final RedissonClient redissonClient;

    public RedissionDataHandle(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    public void set(String key, Object value) {
        redissonClient.getBucket(key).set(value);
    }

    public String get(String key) {
        return StringUtil.getStringValue(redissonClient.getBucket(key).get());
    }

    public void delete(String key) {
        redissonClient.getBucket(key).delete();
    }

    public boolean exists(String key) {
        return redissonClient.getBucket(key).isExists();
    }

    public void expire(String key, long time) {
        redissonClient.getBucket(key).expire(getDuration(time, TimeUnit.SECONDS));
    }

    public void set(String key, Object value, long time,TimeUnit timeUnit){
        redissonClient.getBucket(key).set(value, getDuration(time, timeUnit));
    }

    public Duration getDuration(long timeToLive, TimeUnit timeUnit){
        switch (timeUnit) {
            case MINUTES:
                return Duration.ofMinutes(timeToLive);
            case HOURS:
                return Duration.ofHours(timeToLive);
            case DAYS:
                return Duration.ofDays(timeToLive);
            default:
                return Duration.ofSeconds(timeToLive);
        }
    }
}
