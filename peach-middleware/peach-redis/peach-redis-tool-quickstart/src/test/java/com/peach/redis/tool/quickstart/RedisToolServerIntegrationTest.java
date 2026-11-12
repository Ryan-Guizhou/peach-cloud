package com.peach.redis.tool.quickstart;

import com.peach.redis.tool.RedisDao;
import com.peach.redis.tool.RedisDaoImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 用真实 Redis 7.2 验证不依赖旧 RedisConfig 的 Tool Starter 字符串、TTL、SCAN 与删除。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/10
 */
@EnabledIfEnvironmentVariable(named = "RUN_REDIS_CLIENT_IT", matches = "true")
@SpringBootTest(properties = {
        "quickstart.redis-tool.demo.enabled=false",
        "peach.redis.mode=standalone",
        "peach.redis.host=${REDIS_TOOL_IT_HOST:127.0.0.1:6379}",
        "peach.redis.database=0",
        "peach.redis.password="
})
class RedisToolServerIntegrationTest {

    @Autowired
    private RedisConnectionFactory connectionFactory;

    @Test
    void allOperationsAndCleanupModesShouldWork() throws Exception {
        RedisToolScenarioAssertions.verify(redisDao, connectionFactory);
    }

    @Autowired
    private RedisDao redisDao;

    @Autowired
    private StringRedisTemplate strings;

    @Autowired
    private ApplicationContext context;

    @Test
    void toolStarterShouldWorkWithoutLegacyClientBeans() {
        assertThat(context.getBeansOfType(RedisConnectionFactory.class)).hasSize(1);
        assertThat(context.containsBean("redissonClient")).isFalse();

        String prefix = "tool:it:" + UUID.randomUUID() + ":";
        String first = prefix + "one";
        String second = prefix + "two";
        String unrelated = "tool:keep:" + UUID.randomUUID();
        try {
            assertThat(redisDao.vSet(first, "one", 30L)).isTrue();
            assertThat(redisDao.vSet(second, "two", 30L)).isTrue();
            strings.opsForValue().set(unrelated, "preserve");

            assertThat(redisDao.vGet(first)).isEqualTo("one");
            assertThat(strings.getExpire(first)).isBetween(1L, 30L);
            assertThat(redisDao.scan(prefix + "*", 100)).containsExactlyInAnyOrder(first, second);
            // 继续使用同一个真实 Redis 测试，不增加单独的慢速测试套件。
            RedisDaoImpl limitedDao = new RedisDaoImpl(context.getBean("redisTemplate", RedisTemplate.class));
            ReflectionTestUtils.setField(limitedDao, "scanMaxResults", 1);
            assertThatThrownBy(() -> limitedDao.scan(prefix + "*", 100))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("result limit");
            assertThatThrownBy(() -> redisDao.deletePattern("*", 100))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("business namespace");
            assertThat(redisDao.deletePattern(prefix + "*", 100)).isEqualTo(2L);
            assertThat(strings.opsForValue().get(unrelated)).isEqualTo("preserve");
        } finally {
            strings.delete(first);
            strings.delete(second);
            strings.delete(unrelated);
        }
    }
}
