package com.peach.satoken.quickstart;

import com.peach.redis.autoconfigure.PeachRedisAutoConfiguration;
import com.peach.redis.autoconfigure.PeachRedisProperties;
import com.peach.satoken.dao.PeachSaTokenDao;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import redis.clients.jedis.JedisPoolConfig;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 使用真实 Sentinel、Cluster 与 TLS 客户端验收 Sa-Token SCAN 跨分片查询。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/10
 */
@EnabledIfEnvironmentVariable(named = "RUN_REDIS_TOPOLOGY_IT", matches = "true")
class SaTokenSearchTopologyServerIntegrationTest {

    @Test
    void independentFactoryFindsSessionKeysAcrossTopology() {
        PeachRedisProperties properties = new PeachRedisProperties();
        properties.setMode(System.getenv("REDIS_TOPOLOGY_MODE"));
        properties.setHost(System.getenv("REDIS_TOPOLOGY_HOSTS"));
        properties.setDatabase(0);
        properties.setSentinelMaster(System.getenv().getOrDefault("REDIS_TOPOLOGY_SENTINEL_MASTER", "master"));
        properties.setSslEnabled(Boolean.parseBoolean(System.getenv("REDIS_TOPOLOGY_TLS")));

        JedisConnectionFactory factory = new PeachRedisAutoConfiguration()
                .jedisConnectionFactory(new JedisPoolConfig(), properties);
        factory.afterPropertiesSet();
        PeachSaTokenDao dao = new PeachSaTokenDao(factory, 100);
        dao.init();
        String prefix = "satoken:topology:" + UUID.randomUUID() + ":";
        String first = prefix + "{left}:session";
        String second = prefix + "{right}:session";
        try {
            dao.setObject(first, Map.of("session", "alpha"), 50);
            dao.setObject(second, Map.of("session", "beta"), 50);
            List<String> found = dao.searchData(prefix, "session", 0, 10, true);
            assertThat(found).containsExactlyInAnyOrder(first, second);
            assertThat(dao.getObjectTimeout(first)).isBetween(1L, 50L);
        } finally {
            dao.deleteObject(first);
            dao.deleteObject(second);
            factory.destroy();
        }
    }
}
