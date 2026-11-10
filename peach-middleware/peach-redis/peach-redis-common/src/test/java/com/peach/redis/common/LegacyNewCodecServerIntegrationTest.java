package com.peach.redis.common;

import com.peach.redis.autoconfigure.PeachRedisAutoConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 在真实 Redis 7.2 上验证当前仓库旧 RedisConfig 与新 Jedis 自动装配的存储字节兼容。
 *
 * <p>不代表旧发行版二进制所创建的历史 Sa-Token Session 已通过滚动升级验收。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/10
 */
@EnabledIfEnvironmentVariable(named = "RUN_REDIS_CLIENT_IT", matches = "true")
class LegacyNewCodecServerIntegrationTest {

    private JedisConnectionFactory connectionFactory;
    private RedisTemplate<String, Object> legacy;
    private RedisTemplate<String, Object> modern;
    private final List<String> keys = new ArrayList<>();

    @BeforeEach
    void connect() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration("127.0.0.1", 6379);
        config.setDatabase(0);
        connectionFactory = new JedisConnectionFactory(config);
        connectionFactory.afterPropertiesSet();
        legacy = new RedisConfig().redisTemplate(connectionFactory);
        modern = new PeachRedisAutoConfiguration().redisTemplate(connectionFactory);
    }

    @AfterEach
    void cleanUp() {
        if (modern != null) {
            for (String key : keys) {
                modern.delete(key);
            }
        }
        if (connectionFactory != null) {
            connectionFactory.destroy();
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void valueBytesMatchAndCurrentReaderCanReadLegacyData() {
        Map<String, Object> payload = sessionLikePayload();
        RedisSerializer<Object> legacyValue = (RedisSerializer<Object>) legacy.getValueSerializer();
        RedisSerializer<Object> modernValue = (RedisSerializer<Object>) modern.getValueSerializer();

        assertThat(legacyValue.serialize(payload)).isEqualTo(modernValue.serialize(payload));
        String key = uniqueKey("value");
        legacy.opsForValue().set(key, payload, Duration.ofSeconds(60));

        assertThat(modern.opsForValue().get(key)).isEqualTo(payload);
        assertThat(modern.getExpire(key)).isBetween(1L, 60L);
    }

    @Test
    @SuppressWarnings("unchecked")
    void newHashValueIsReadableByLegacyTemplateAndPreservesTtl() {
        Map<String, Object> payload = sessionLikePayload();
        RedisSerializer<Object> legacyHash = (RedisSerializer<Object>) legacy.getHashValueSerializer();
        RedisSerializer<Object> modernHash = (RedisSerializer<Object>) modern.getHashValueSerializer();
        assertThat(legacyHash.serialize(payload)).isEqualTo(modernHash.serialize(payload));

        String key = uniqueKey("hash");
        modern.opsForHash().put(key, "old-session", payload);
        modern.expire(key, Duration.ofSeconds(60));

        assertThat(legacy.opsForHash().get(key, "old-session")).isEqualTo(payload);
        assertThat(legacy.getExpire(key)).isBetween(1L, 60L);
    }

    private String uniqueKey(String suffix) {
        String key = "redis:codec:compat:it:" + UUID.randomUUID() + ":" + suffix;
        keys.add(key);
        return key;
    }

    private static Map<String, Object> sessionLikePayload() {
        Map<String, Object> result = new HashMap<>();
        result.put("token", "fixture-not-a-real-token");
        result.put("userId", 123L);
        result.put("roles", List.of("viewer", "editor"));
        return result;
    }
}
