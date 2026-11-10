package com.peach.redis.quickstart;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 验证隔离 Redis 7.2 ACL 实际关闭默认用户，错误密码与匿名访问均被拒绝。
 *
 * <p>专用 CI 测试要求正确凭据的独立 Jedis、Redisson、Tool 和 Sa-Token
 * 套件都先成功，才能确认拒绝是认证导致，而不是节点不可达。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/10
 */
@EnabledIfEnvironmentVariable(named = "RUN_REDIS_ACL_IT", matches = "true")
class RedisAclAuthenticationServerIntegrationTest {

    @Test
    void wrongAclPasswordIsRejected() {
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration("127.0.0.1", 6390);
        configuration.setUsername(System.getenv("REDIS_TOPOLOGY_USERNAME"));
        configuration.setPassword(RedisPassword.of("intentionally-invalid"));
        assertAuthenticationRejected(configuration);
    }

    @Test
    void disabledDefaultUserCannotReadWithoutCredentials() {
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration("127.0.0.1", 6390);
        assertAuthenticationRejected(configuration);
    }

    private static void assertAuthenticationRejected(RedisStandaloneConfiguration configuration) {
        JedisConnectionFactory factory = new JedisConnectionFactory(configuration);
        try {
            assertThatThrownBy(() -> {
                factory.afterPropertiesSet();
                StringRedisTemplate template = new StringRedisTemplate(factory);
                template.afterPropertiesSet();
                template.opsForValue().get("peach:ci:acl:nonexistent");
            }).as("Redis ACL server must reject invalid or missing credentials")
                    .isInstanceOf(RuntimeException.class);
        } finally {
            try {
                factory.destroy();
            } catch (RuntimeException ignored) {
                // Rejecting credentials must not leak a partially initialized Jedis pool.
            }
        }
    }
}
