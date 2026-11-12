package com.peach.bootregression;

import com.peach.redis.bloom.autoconfigure.BloomFilterAutoConfiguration;
import com.peach.redis.bloom.core.BloomFilterService;
import com.peach.redisson.autoconfigure.PeachRedissonAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * 验证布隆 Starter 独立接入、自定义客户端名称和能力开关。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/12 11:00
 */
class BloomFilterStarterTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(BloomFilterAutoConfiguration.class,
                    PeachRedissonAutoConfiguration.class));

    @Test
    void clientCanHaveApplicationDefinedName() {
        RedissonClient client = mock(RedissonClient.class);
        runner.withBean("applicationRedisClient", RedissonClient.class, () -> client)
                .run(context -> {
                    assertThat(context).hasNotFailed().hasSingleBean(RedissonClient.class)
                            .hasSingleBean(BloomFilterService.class);
                    verifyNoInteractions(client);
                });
    }

    @Test
    void userBloomServiceTakesPrecedence() {
        BloomFilterService service = mock(BloomFilterService.class);
        runner.withBean("applicationRedisClient", RedissonClient.class, () -> mock(RedissonClient.class))
                .withBean(BloomFilterService.class, () -> service)
                .run(context -> {
                    assertThat(context).hasNotFailed().hasSingleBean(BloomFilterService.class);
                    assertThat(context.getBean(BloomFilterService.class)).isSameAs(service);
                });
    }

    @Test
    void disabledBloomAndClientCreateNoRuntime() {
        runner.withPropertyValues("peach.redis.bloom.enabled=false", "peach.redis.redisson.enabled=false")
                .run(context -> assertThat(context).hasNotFailed().doesNotHaveBean(BloomFilterService.class)
                        .doesNotHaveBean(RedissonClient.class));
    }
}
