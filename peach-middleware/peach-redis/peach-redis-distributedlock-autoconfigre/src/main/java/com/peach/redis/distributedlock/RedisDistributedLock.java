package com.peach.redis.distributedlock;

import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.StaticScriptSource;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;

public class RedisDistributedLock {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisDistributedLockProperties properties;

    public RedisDistributedLock(RedisTemplate<String, Object> redisTemplate, RedisDistributedLockProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    public boolean tryLock(String name, String token, Duration ttl) {
        String key = buildKey(name);
        Duration useTtl = ttl == null ? properties.getDefaultTtl() : ttl;
        Boolean ok = redisTemplate.opsForValue().setIfAbsent(key, token, useTtl);
        return Boolean.TRUE.equals(ok);
    }

    public boolean unlock(String name, String token) {
        String key = buildKey(name);
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText("if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end");
        script.setResultType(Long.class);
        Long res = redisTemplate.execute(script, Collections.singletonList(key), token);
        return res != null && res > 0;
    }

    private String buildKey(String name) {
        return properties.getKeyPrefix() + ":" + name;
    }
}

