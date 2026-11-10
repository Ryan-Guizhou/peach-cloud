package com.peach.satoken.autoconfigure;

import cn.dev33.satoken.dao.SaTokenDao;
import com.peach.satoken.dao.PeachSaTokenDao;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * 验证 Sa-Token 仅依赖 RedisConnectionFactory 接口，不再需要旧 RedisConfig/Jedis Bean。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/10
 */
class PeachSaTokenRedisFactoryCompatibilityTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(PeachSaTokenDaoAutoConfiguration.class));

    @Test
    void acceptsAnyRedisConnectionFactoryWithoutLegacyJedisBean() {
        RedisConnectionFactory factory = mock(RedisConnectionFactory.class);
        runner.withBean(RedisConnectionFactory.class, () -> factory)
                .withPropertyValues("peach.satoken.dao.enabled=true")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(SaTokenDao.class)).isInstanceOf(PeachSaTokenDao.class);
                    assertThat(context.containsBean("jedisConnectionFactory")).isFalse();
                });
    }

    @Test
    void disabledDaoDoesNotRegisterPersistenceImplementation() {
        runner.withPropertyValues("peach.satoken.dao.enabled=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBeansOfType(SaTokenDao.class)).isEmpty();
                });
    }
}
