package com.peach.bootregression;

import com.peach.redission.distrbutedlock.aspect.DistrbutedLockAspect;
import com.peach.redission.distrbutedlock.manage.DistrbutedLockerFactory;
import com.peach.redission.distrbutedlock.manage.DistrbutedLockerManager;
import com.peach.redission.distrbutedlock.support.DistributedLockTemplate;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

/**
 * 在独立 Starter 类路径中验证扫描边界、锁装配链和用户覆盖，不连接外部 Redis。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/12 11:00
 */
class DistributedLockStarterTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withPropertyValues("peach.redis.mode=standalone", "peach.redis.host=127.0.0.1:6379");

    @Test
    void lockStarterDoesNotRequireJedisOrSpringDataRedis() {
        assertThat(ClassUtils.isPresent("redis.clients.jedis.Jedis", getClass().getClassLoader())).isFalse();
        assertThat(ClassUtils.isPresent("org.springframework.data.redis.core.RedisTemplate", getClass().getClassLoader()))
                .isFalse();
    }

    @Test
    void explicitComponentScanReproducesMissingTemplateDespiteClientBeingPresent() {
        RedissonClient client = mock(RedissonClient.class);
        try (MockedStatic<Redisson> redisson = mockStatic(Redisson.class)) {
            redisson.when(() -> Redisson.create(any(Config.class))).thenReturn(client);
            runner.withUserConfiguration(LegacyApplication.class).run(context -> {
                assertThat(context).hasNotFailed().hasSingleBean(RedissonClient.class);
                assertThat(context).doesNotHaveBean(DistributedLockTemplate.class);
            });
        }
    }

    @Test
    void bootScanCreatesCompleteLockChainAndClosesItsClient() {
        RedissonClient client = mock(RedissonClient.class);
        try (MockedStatic<Redisson> redisson = mockStatic(Redisson.class)) {
            redisson.when(() -> Redisson.create(any(Config.class))).thenReturn(client);
            runner.withUserConfiguration(BootApplication.class).run(context -> {
                assertThat(context).hasNotFailed()
                        .hasSingleBean(RedissonClient.class)
                        .hasSingleBean(DistrbutedLockerManager.class)
                        .hasSingleBean(DistrbutedLockerFactory.class)
                        .hasSingleBean(DistributedLockTemplate.class)
                        .hasSingleBean(DistrbutedLockAspect.class);
                redisson.verify(() -> Redisson.create(any(Config.class)));
            });
            org.mockito.Mockito.verify(client).shutdown();
        }
    }

    @Test
    void userClientAndTemplateTakePrecedence() {
        RedissonClient client = mock(RedissonClient.class);
        DistributedLockTemplate template = mock(DistributedLockTemplate.class);
        runner.withUserConfiguration(BootApplication.class)
                .withBean("customClient", RedissonClient.class, () -> client)
                .withBean(DistributedLockTemplate.class, () -> template)
                .run(context -> {
                    assertThat(context).hasNotFailed().hasSingleBean(RedissonClient.class)
                            .hasSingleBean(DistributedLockTemplate.class);
                    assertThat(context.getBean(RedissonClient.class)).isSameAs(client);
                    assertThat(context.getBean(DistributedLockTemplate.class)).isSameAs(template);
                });
    }

    @Test
    void disablingDefaultClientStillAllowsUserClient() {
        runner.withUserConfiguration(BootApplication.class)
                .withPropertyValues("peach.redis.redisson.enabled=false")
                .withBean("customClient", RedissonClient.class, () -> mock(RedissonClient.class))
                .run(context -> assertThat(context).hasNotFailed().hasSingleBean(DistributedLockTemplate.class));
    }

    @Test
    void disablingClientWithoutReplacementFailsInsteadOfSilentlyRemovingLockProtection() {
        runner.withUserConfiguration(BootApplication.class)
                .withPropertyValues("peach.redis.redisson.enabled=false")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(NoSuchBeanDefinitionException.class)
                            .hasMessageContaining("RedissonClient");
                });
    }

    /**
     * 复现存量启动入口覆盖 Boot 扫描过滤器的行为。
     * @Author Mr Shu
     * @Version 1.0.0
     * @CreateTime 2026/11/12 11:00
     */
    @SpringBootApplication(scanBasePackages = {"com.peach.redission", "com.peach.redisson.autoconfigure"})
    @ComponentScan({"com.peach.redission", "com.peach.redisson.autoconfigure"})
    static class LegacyApplication {
    }

    /**
     * 保留 Boot 扫描过滤器，自动配置由 imports 文件注册。
     * @Author Mr Shu
     * @Version 1.0.0
     * @CreateTime 2026/11/12 11:00
     */
    @SpringBootApplication(scanBasePackages = {"com.peach.redission", "com.peach.redisson.autoconfigure"})
    static class BootApplication {
    }
}
