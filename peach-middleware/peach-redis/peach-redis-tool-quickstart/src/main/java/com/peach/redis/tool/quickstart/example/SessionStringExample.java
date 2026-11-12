package com.peach.redis.tool.quickstart.example;

import com.peach.redis.tool.RedisDao;
import com.peach.redis.tool.quickstart.config.RedisToolDemoContext;
import com.peach.redis.tool.quickstart.enums.RedisToolKey;
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

    private final RedisDao redisDao;

    private final RedisToolDemoContext context;

    public void save(String sessionId, String token, Duration ttl) {
        redisDao.vSet(getRealKey(sessionId), token, ttl);
        log.info("session save, sessionId={}, ttl={}", sessionId, ttl);
    }

    public String get(String sessionId) {
        String realKey = getRealKey(sessionId);
        Object value = redisDao.vGet(realKey);
        String token = value == null ? null : String.valueOf(value);
        log.info("session get, sessionId={}, hit={}", sessionId, token != null);
        return token;
    }

    public boolean exists(String sessionId) {
        return redisDao.existsKey(getRealKey(sessionId));
    }

    public void delete(String sessionId) {
        redisDao.delete(getRealKey(sessionId));
        log.info("session delete, sessionId={}", sessionId);
    }

    private String getRealKey(String sessionId) {
        return context.key(RedisToolKey.SESSION, sessionId);
    }
}
