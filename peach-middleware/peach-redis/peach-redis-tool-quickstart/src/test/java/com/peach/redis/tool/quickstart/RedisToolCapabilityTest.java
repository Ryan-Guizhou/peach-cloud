package com.peach.redis.tool.quickstart;

import com.peach.redis.tool.RedisDao;
import org.springframework.data.redis.core.StringRedisTemplate;
import com.peach.redis.tool.quickstart.example.SessionStringExample;
import com.peach.redis.tool.quickstart.example.UserProfileHashExample;
import com.peach.redis.tool.quickstart.example.UserTagSetExample;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.UUID;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import com.peach.redis.tool.RedisDaoImpl;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证 RedisDao 全方法示例覆盖、清理配置及集合扫描预算。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 17:50
 */
@SpringBootTest(properties = "quickstart.redis-tool.demo.enabled=false")
@Testcontainers(disabledWithoutDocker = true)
class RedisToolCapabilityTest {

    @Autowired
    private org.springframework.data.redis.connection.RedisConnectionFactory connectionFactory;

    @Test
    void allOperationsAndCleanupModesShouldWork() throws Exception {
        RedisToolScenarioAssertions.verify(redisDao, connectionFactory);
    }

    @Container
    @SuppressWarnings("resource")
    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7.2-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("peach.redis.mode", () -> "standalone");
        registry.add("peach.redis.host", () -> REDIS.getHost() + ":" + REDIS.getMappedPort(6379));
        registry.add("peach.redis.password", () -> "");
        registry.add("peach.redis.database", () -> "0");
    }

    @Autowired
    private SessionStringExample sessionStringExample;

    @Autowired
    private RedisDao redisDao;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private UserProfileHashExample userProfileHashExample;

    @Autowired
    private UserTagSetExample userTagSetExample;

    @BeforeEach
    void clean() {
        sessionStringExample.delete("s-1001");
        userProfileHashExample.deleteProfile("u-2001");
        userTagSetExample.clear("u-2001");
    }

    @Test
    void sessionStringShouldSaveGetAndDelete() {
        sessionStringExample.save("s-1001", "token-abc", Duration.ofMinutes(10));
        assertThat(sessionStringExample.exists("s-1001")).isTrue();
        assertThat(sessionStringExample.get("s-1001")).isEqualTo("token-abc");

        sessionStringExample.delete("s-1001");
        assertThat(sessionStringExample.exists("s-1001")).isFalse();
        assertThat(sessionStringExample.get("s-1001")).isNull();
    }

    @Test
    void ttlWriteShouldBeAtomicAndExpire() {
        String key = "test:redisdao:ttl";
        try {
            assertThat(redisDao.vSet(key, "expiring", 30L)).isTrue();
            assertThat(stringRedisTemplate.getExpire(key)).isBetween(1L, 30L);
        } finally {
            redisDao.delete(key);
        }
    }

    @Test
    void collectionScansShouldUseActualKeysAndEnforceLimits() {
        String prefix = "test:collection:scan:" + UUID.randomUUID();
        String hash = prefix + ":hash";
        String set = prefix + ":set";
        String zset = prefix + ":zset";
        try {
            redisDao.hmSet(hash, "scan-a", "one");
            redisDao.hmSet(hash, "scan-b", "two");
            redisDao.sAdd(set, "alpha");
            redisDao.sAdd(set, "beta");
            redisDao.zAdd(zset, "first", 1.0);
            redisDao.zAdd(zset, "second", 2.0);

            assertThat(redisDao.hmScan(hash, 1))
                    .containsEntry("scan-a", "one").containsEntry("scan-b", "two");
            assertThat(redisDao.sScan(set, 1)).containsExactlyInAnyOrder("alpha", "beta");
            assertThat(redisDao.zScan(zset, 1))
                    .containsEntry("first", 1.0).containsEntry("second", 2.0);

            RedisDaoImpl bounded = new RedisDaoImpl(
                    (RedisTemplate<?, ?>) redisDaoTemplate());
            ReflectionTestUtils.setField(bounded, "scanMaxResults", 1);
            assertThatThrownBy(() -> bounded.hmScan(hash, 1))
                    .isInstanceOf(IllegalStateException.class).hasMessageContaining("result limit");
            assertThatThrownBy(() -> bounded.sScan(set, 1))
                    .isInstanceOf(IllegalStateException.class).hasMessageContaining("result limit");
            assertThatThrownBy(() -> bounded.zScan(zset, 1))
                    .isInstanceOf(IllegalStateException.class).hasMessageContaining("result limit");
        } finally {
            redisDao.delete(hash);
            redisDao.delete(set);
            redisDao.delete(zset);
        }
    }

    @Autowired
    private RedisTemplate<?, ?> redisTemplate;

    private RedisTemplate<?, ?> redisDaoTemplate() {
        return redisTemplate;
    }

    @Test
    void missingKeyShouldNotReportSuccessfulTtlUpdate() {
        String key = "test:redisdao:missing:" + UUID.randomUUID();
        assertThat(redisDao.expire(key, Duration.ofSeconds(30))).isFalse();
        assertThat(redisDao.vSet(key, "value", 30L)).isTrue();
        assertThat(redisDao.expire(key, Duration.ofSeconds(20))).isTrue();
        redisDao.delete(key);
    }

    @Test
    void scanShouldFindKeysAndDeletePatternWithoutTouchingOtherKeys() {
        String prefix = "test:scan:23:";
        String first = prefix + "first";
        String second = prefix + "second";
        String unrelated = "test:scan:keep";
        try {
            stringRedisTemplate.opsForValue().set(first, "one");
            stringRedisTemplate.opsForValue().set(second, "two");
            stringRedisTemplate.opsForValue().set(unrelated, "keep");

            assertThat(redisDao.scan(prefix + "*", null)).containsExactlyInAnyOrder(first, second);
            assertThat(redisDao.deletePattern(prefix + "*", 2)).isEqualTo(2L);
            assertThat(stringRedisTemplate.hasKey(first)).isFalse();
            assertThat(stringRedisTemplate.hasKey(second)).isFalse();
            assertThat(stringRedisTemplate.opsForValue().get(unrelated)).isEqualTo("keep");
        } finally {
            stringRedisTemplate.delete(first);
            stringRedisTemplate.delete(second);
            stringRedisTemplate.delete(unrelated);
        }
    }

    @Test
    void profileHashShouldWriteReadAndFieldDelete() {
        userProfileHashExample.saveProfile("u-2001", "Peach", "Shanghai");
        assertThat(userProfileHashExample.getNickname("u-2001")).isEqualTo("Peach");

        Map<Object, Object> all = userProfileHashExample.getAll("u-2001");
        assertThat(all).containsEntry("nickname", "Peach").containsEntry("city", "Shanghai");

        userProfileHashExample.removeCity("u-2001");
        assertThat(userProfileHashExample.getAll("u-2001")).containsKey("nickname").doesNotContainKey("city");
    }

    @Test
    void tagSetShouldAddListAndRemove() {
        userTagSetExample.addTags("u-2001", "vip", "beta", "cn");
        Set<Object> tags = userTagSetExample.listTags("u-2001");
        assertThat(tags).contains("vip", "beta", "cn");

        userTagSetExample.removeTag("u-2001", "beta");
        assertThat(userTagSetExample.listTags("u-2001")).contains("vip", "cn").doesNotContain("beta");
    }
}
