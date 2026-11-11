package com.peach.captcha.service.impl.cache;

import com.peach.captcha.service.CaptchaCacheService;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;


/**
 * Redis验证码缓存服务类。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:55
 */
public class RedisCaptchaCacheService implements CaptchaCacheService {

    private static final RedisScript<Long> INCREMENT_SCRIPT = RedisScript.of(
            new ClassPathResource("lua/captcha-increment-with-ttl.lua"), Long.class);

    private final StringRedisTemplate stringRedisTemplate;

    public RedisCaptchaCacheService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public void set(String key, String value, long expiresInSeconds) {
        stringRedisTemplate.opsForValue().set(key, value, expiresInSeconds, TimeUnit.SECONDS);
    }

    @Override
    public boolean exists(String key) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(key));
    }

    @Override
    public void delete(String key) {
        stringRedisTemplate.delete(key);
    }

    @Override
    public String get(String key) {
        return stringRedisTemplate.opsForValue().get(key);
    }

    @Override
    public Long increment(String key, long val) {
        return incrementAndExpire(key, val, 60L);
    }

    @Override
    public Long incrementAndExpire(String key, long val, long expiresInSeconds) {
        if (val <= 0 || expiresInSeconds <= 0) {
            throw new IllegalArgumentException("Increment and expiration must be positive");
        }
        return Objects.requireNonNull(stringRedisTemplate.execute(INCREMENT_SCRIPT,
                List.of(key), String.valueOf(val), String.valueOf(expiresInSeconds)),
                "Redis captcha counter increment returned no result");
    }

    @Override
    public String getAndDelete(String key) {
        return stringRedisTemplate.opsForValue().getAndDelete(key);
    }
}
