package com.peach.redis.quickstart;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 对独立 Jedis 底座执行真实 Sentinel、Cluster、TLS 拓扑回归；不依赖 mock。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/10
 */
@EnabledIfEnvironmentVariable(named = "RUN_REDIS_TOPOLOGY_IT", matches = "true")
@SpringBootTest(properties = {
        "peach.redis.mode=${REDIS_TOPOLOGY_MODE}",
        "peach.redis.host=${REDIS_TOPOLOGY_HOSTS}",
        "peach.redis.sentinelMaster=${REDIS_TOPOLOGY_SENTINEL_MASTER:master}",
        "peach.redis.database=0",
        "peach.redis.username=${REDIS_TOPOLOGY_USERNAME:}",
        "peach.redis.password=${REDIS_TOPOLOGY_PASSWORD:}",
        "peach.redis.ssl-enabled=${REDIS_TOPOLOGY_TLS:false}",
        "quickstart.redis.demo.enabled=false"
})
class RedisTopologyServerIntegrationTest {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private StringRedisTemplate redis;

    @Test
    void redisOnlyStarterPersistsOnConfiguredTopology() {
        assertThat(context.getBeansOfType(RedisConnectionFactory.class)).hasSize(1);
        assertThat(context.getBean(RedisConnectionFactory.class)).isInstanceOf(JedisConnectionFactory.class);
        assertThat(context.containsBean("redissonClient")).isFalse();

        String key = "topology:redis-only:" + UUID.randomUUID();
        try {
            redis.opsForValue().set(key, "verified", Duration.ofSeconds(40));
            assertThat(redis.opsForValue().get(key)).isEqualTo("verified");
            assertThat(redis.getExpire(key)).isBetween(1L, 40L);
        } finally {
            redis.delete(key);
        }
    }
}
