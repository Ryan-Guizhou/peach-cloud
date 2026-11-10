package com.peach.redisson.quickstart;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 对独立 Redisson 底座执行真实 Sentinel、Cluster、TLS 拓扑回归。
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
        "peach.redis.redisson.ssl-enable=${REDIS_TOPOLOGY_TLS:false}",
        "quickstart.redisson.demo.enabled=false"
})
class RedissonTopologyServerIntegrationTest {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private RedissonClient redisson;

    @Test
    void redissonOnlyStarterPersistsOnConfiguredTopology() {
        assertThat(context.getBeansOfType(RedissonClient.class)).hasSize(1);
        assertThat(context.containsBean("jedisConnectionFactory")).isFalse();
        assertThat(context.containsBean("redisTemplate")).isFalse();

        String key = "topology:redisson-only:" + UUID.randomUUID();
        try {
            redisson.getBucket(key).set("verified", 40, TimeUnit.SECONDS);
            assertThat(redisson.getBucket(key).get()).isEqualTo("verified");
            assertThat(redisson.getBucket(key).remainTimeToLive()).isGreaterThan(0);
        } finally {
            redisson.getBucket(key).delete();
        }
    }
}
