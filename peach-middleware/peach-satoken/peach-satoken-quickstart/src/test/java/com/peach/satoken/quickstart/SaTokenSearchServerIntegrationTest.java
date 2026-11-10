package com.peach.satoken.quickstart;

import com.peach.satoken.dao.PeachSaTokenDao;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 在真实 Redis 7.2 验证 Sa-Token 对象键的 SCAN、分页与 TTL。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/10
 */
@EnabledIfEnvironmentVariable(named = "RUN_REDIS_CLIENT_IT", matches = "true")
class SaTokenSearchServerIntegrationTest {

    @Test
    void searchPersistedKeysWithoutKeysCommandAndEnforceBudget() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration("127.0.0.1", 6379);
        config.setDatabase(0);
        JedisConnectionFactory factory = new JedisConnectionFactory(config);
        factory.afterPropertiesSet();
        PeachSaTokenDao dao = new PeachSaTokenDao(factory, 3);
        dao.init();
        String prefix = "satoken:search:it:" + UUID.randomUUID() + ":";
        String first = prefix + "alpha:session";
        String second = prefix + "beta:session";
        String third = prefix + "gamma:session";
        try {
            dao.setObject(first, Map.of("id", "a"), 90);
            dao.setObject(second, Map.of("id", "b"), 90);
            dao.setObject(third, Map.of("id", "c"), 90);

            assertThat(dao.searchData(prefix, "session", 0, 2, true))
                    .containsExactly(first, second);
            assertThat(dao.searchData(prefix, "session", 1, 2, false))
                    .containsExactly(second, first);
            assertThat(dao.searchData(prefix, "missing", 0, 10, true)).isEmpty();
            assertThat(dao.getObjectTimeout(first)).isBetween(1L, 90L);

            PeachSaTokenDao capped = new PeachSaTokenDao(factory, 2);
            capped.init();
            assertThatThrownBy(() -> capped.searchData(prefix, "session", 0, -1, true))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("limit exceeded");
        } finally {
            dao.deleteObject(first);
            dao.deleteObject(second);
            dao.deleteObject(third);
            factory.destroy();
        }
    }
}
