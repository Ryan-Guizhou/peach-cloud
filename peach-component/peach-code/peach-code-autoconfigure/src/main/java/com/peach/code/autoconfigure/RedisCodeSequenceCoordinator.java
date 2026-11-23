package com.peach.code.autoconfigure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

/**
 * 使用原子脚本在数据库锁内合并 Redis 水位，不设置过期时间。
 *
 * <p>脚本以十进制字符串比较，避免 Lua 浮点数损失 18 位序号精度；INCR 后 GET 返回精确值。
 * Redis 故障或无效缓存均退回当前数据库锁内的持久水位，未提交的 Redis 预留可产生空号。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/23 10:45
 */
final class RedisCodeSequenceCoordinator implements CodeSequenceCoordinator {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisCodeSequenceCoordinator.class);
    private static final DefaultRedisScript<String> ALLOCATE = new DefaultRedisScript<>("""
            local value = redis.call('GET', KEYS[1])
            if not value or not string.match(value, '^%d+$') or #value > 18 then
                value = ARGV[1]
            end
            value = string.gsub(value, '^0+', '')
            if value == '' then value = '0' end
            local function less(a,b)
                return #a < #b or (#a == #b and a < b)
            end
            if less(value, ARGV[1]) then value = ARGV[1] end
            if not less(value, ARGV[2]) then return '-1' end
            redis.call('SET', KEYS[1], value)
            redis.call('INCR', KEYS[1])
            return redis.call('GET', KEYS[1])
            """, String.class);

    private final StringRedisTemplate redis;
    private final String keyPrefix;

    RedisCodeSequenceCoordinator(StringRedisTemplate redis, CodeProperties properties) {
        this.redis = redis;
        this.keyPrefix = properties.getRedisKeyPrefix();
        if (keyPrefix == null || keyPrefix.isBlank()) {
            throw new IllegalArgumentException("Redis code key prefix must not be blank");
        }
    }

    @Override
    public long next(String tenantId, String codeType, long durable, long maximum) {
        try {
            String value = redis.execute(ALLOCATE, List.of(keyPrefix + digest(tenantId) + ":" + codeType),
                    Long.toString(durable), Long.toString(maximum));
            return value == null ? durable + 1 : Long.parseLong(value);
        } catch (DataAccessException | NumberFormatException ex) {
            LOGGER.warn("Redis code coordination unavailable; continuing under the MySQL rule lock");
            return durable + 1;
        }
    }

    private static String digest(String tenantId) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(tenantId.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is required", ex);
        }
    }
}
