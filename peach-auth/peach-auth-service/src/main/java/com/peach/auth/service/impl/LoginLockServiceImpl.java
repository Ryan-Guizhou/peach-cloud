package com.peach.auth.service.impl;

import com.peach.auth.service.LoginLockService;
import com.peach.auth.vo.LoginLockStatusVO;
import com.peach.common.util.StringUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Indexed;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

/**
 * 基于 Redis Lua 的登录失败锁定实现。
 */
@Indexed
@Service
@RequiredArgsConstructor
public class LoginLockServiceImpl implements LoginLockService {

    private static final String KEY_PREFIX = "peach:auth:login:lock:";

    private static final int LOCK_THRESHOLD = 5;

    private static final int PERMANENT_THRESHOLD = 10;

    private static final int INITIAL_LOCK_SECONDS = 30 * 60;

    private static final String CHECK_LOCK_LUA = """
            local permanent = redis.call('HGET', KEYS[1], 'permanent')
            if permanent == '1' then
              local failCount = redis.call('HGET', KEYS[1], 'failCount') or '0'
              return {'LOCKED', 'PERMANENT', '0', failCount}
            end
            local lockUntil = tonumber(redis.call('HGET', KEYS[1], 'lockUntil') or '0')
            local now = tonumber(ARGV[1])
            if lockUntil > now then
              local failCount = redis.call('HGET', KEYS[1], 'failCount') or '0'
              return {'LOCKED', 'TEMPORARY', tostring(lockUntil), failCount}
            end
            return {'OK'}
            """;

    private static final String RECORD_FAILURE_LUA = """
            local key = KEYS[1]
            local now = tonumber(ARGV[1])
            local initialLockSec = tonumber(ARGV[2])
            local lockThreshold = tonumber(ARGV[3])
            local permanentThreshold = tonumber(ARGV[4])
            local failCount = tonumber(redis.call('HGET', key, 'failCount') or '0') + 1
            redis.call('HSET', key, 'failCount', failCount)
            if failCount >= permanentThreshold then
              redis.call('HSET', key, 'permanent', '1')
              redis.call('HDEL', key, 'lockUntil')
              redis.call('PERSIST', key)
              return {failCount, -1, 1}
            end
            if failCount >= lockThreshold then
              local exponent = failCount - lockThreshold
              local lockSec = initialLockSec * (2 ^ exponent)
              local lockUntil = now + lockSec * 1000
              redis.call('HSET', key, 'lockUntil', lockUntil)
              redis.call('EXPIRE', key, lockSec + 86400)
              return {failCount, lockUntil, 0}
            end
            redis.call('EXPIRE', key, 86400)
            return {failCount, 0, 0}
            """;

    private static final DefaultRedisScript<List> CHECK_LOCK_SCRIPT = new DefaultRedisScript<>(CHECK_LOCK_LUA, List.class);

    private static final DefaultRedisScript<List> RECORD_FAILURE_SCRIPT =
            new DefaultRedisScript<>(RECORD_FAILURE_LUA, List.class);

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public LoginLockStatusVO checkLock(String account) {
        LoginLockStatusVO status = new LoginLockStatusVO();
        if (StringUtil.isBlank(account)) {
            return status;
        }
        List<?> result = stringRedisTemplate.execute(
                CHECK_LOCK_SCRIPT,
                List.of(buildKey(account)),
                String.valueOf(System.currentTimeMillis()));
        populateStatus(status, result, false);
        return status;
    }

    @Override
    public LoginLockStatusVO recordFailure(String account) {
        LoginLockStatusVO status = new LoginLockStatusVO();
        if (StringUtil.isBlank(account)) {
            return status;
        }
        List<?> result = stringRedisTemplate.execute(
                RECORD_FAILURE_SCRIPT,
                List.of(buildKey(account)),
                String.valueOf(System.currentTimeMillis()),
                String.valueOf(INITIAL_LOCK_SECONDS),
                String.valueOf(LOCK_THRESHOLD),
                String.valueOf(PERMANENT_THRESHOLD));
        populateStatus(status, result, true);
        return status;
    }

    @Override
    public void clearOnSuccess(String account) {
        if (StringUtil.isNotBlank(account)) {
            stringRedisTemplate.delete(buildKey(account));
        }
    }

    private void populateStatus(LoginLockStatusVO status, List<?> result, boolean afterFailure) {
        if (result == null || result.isEmpty()) {
            return;
        }
        if ("LOCKED".equals(String.valueOf(result.get(0)))) {
            status.setLocked(true);
            if ("PERMANENT".equals(String.valueOf(result.get(1)))) {
                status.setPermanent(true);
                status.setFailCount(PERMANENT_THRESHOLD);
            } else {
                long lockUntil = Long.parseLong(String.valueOf(result.get(2)));
                status.setLockUntilEpochMs(lockUntil);
                status.setRemainingLockSeconds(Math.max(0L, (lockUntil - System.currentTimeMillis()) / 1000L));
                if (result.size() > 3) {
                    status.setFailCount(Integer.parseInt(String.valueOf(result.get(3))));
                }
            }
            return;
        }
        if (afterFailure && result.size() >= 3) {
            status.setFailCount(Integer.parseInt(String.valueOf(result.get(0))));
            int permanentFlag = Integer.parseInt(String.valueOf(result.get(2)));
            if (permanentFlag == 1) {
                status.setLocked(true);
                status.setPermanent(true);
                return;
            }
            long lockUntil = Long.parseLong(String.valueOf(result.get(1)));
            if (lockUntil > 0) {
                status.setLocked(true);
                status.setLockUntilEpochMs(lockUntil);
                status.setRemainingLockSeconds(Math.max(0L, (lockUntil - System.currentTimeMillis()) / 1000L));
            }
        }
    }

    private String buildKey(String account) {
        return KEY_PREFIX + sha256(account.trim().toLowerCase());
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
