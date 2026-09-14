package com.peach.redis.tool.quickstart.example;

import com.peach.redis.common.tool.RedisDao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Indexed;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * String + TTL：会话类 KV 写入、读取、存在判断与删除。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 17:50
 */
@Slf4j
@Indexed
@Service
@RequiredArgsConstructor
public class SessionStringExample {

    public static final String KEY_PREFIX = "peach:qs:tool:session:";

    private final RedisDao redisDao;

    public void save(String sessionId, String token, Duration ttl) {
        String key = KEY_PREFIX + sessionId;
        redisDao.vSet(key, token, ttl);
        log.info("session save, sessionId={}, ttl={}", sessionId, ttl);
    }

    public String get(String sessionId) {
        Object value = redisDao.vGet(KEY_PREFIX + sessionId);
        String token = value == null ? null : String.valueOf(value);
        log.info("session get, sessionId={}, hit={}", sessionId, token != null);
        return token;
    }

    public boolean exists(String sessionId) {
        return redisDao.existsKey(KEY_PREFIX + sessionId);
    }

    public void delete(String sessionId) {
        redisDao.delete(KEY_PREFIX + sessionId);
        log.info("session delete, sessionId={}", sessionId);
    }
}
