package com.peach.redis.tool;

import com.peach.redis.tool.autoconfigure.RedisDaoAutoConfiguration;
import com.peach.redis.tool.autoconfigure.RedisToolProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * 验证类型化配置绑定、非法预算拒绝及自定义 DAO 退让。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/12 16:00
 */
class RedisToolAutoConfigurationTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(RedisDaoAutoConfiguration.class)
            .withBean("redisTemplate", RedisTemplate.class, () -> mock(RedisTemplate.class));

    @Test
    void shouldBindExistingScanPropertiesAndBackOff() {
        runner.withPropertyValues("peach.redis.tool.scan.max-results=23",
                "peach.redis.tool.scan.timeout-millis=250").run(context -> {
                    assertThat(context).hasSingleBean(RedisDao.class);
                    RedisToolProperties properties = context.getBean(RedisToolProperties.class);
                    assertThat(properties.getScan().getMaxResults()).isEqualTo(23);
                    assertThat(properties.getScan().getTimeoutMillis()).isEqualTo(250);
                });
        RedisDao custom = mock(RedisDao.class);
        runner.withBean("customDao", RedisDao.class, () -> custom).run(context ->
                assertThat(context.getBean(RedisDao.class)).isSameAs(custom));
    }

    @Test
    void shouldRejectInvalidBudgetAtStartup() {
        runner.withPropertyValues("peach.redis.tool.scan.max-results=0").run(context ->
                assertThat(context).hasFailed());
    }

    @Test
    @SuppressWarnings("unchecked")
    void conditionalWritesMustRejectInvalidTtlAndUnknownResult() {
        RedisTemplate<Object, Object> template = mock(RedisTemplate.class);
        ValueOperations<Object, Object> values = mock(ValueOperations.class);
        when(template.opsForValue()).thenReturn(values);
        RedisDao dao = new RedisDaoImpl(template);
        assertThatThrownBy(() -> dao.vSetIfAbsent("key", "value", Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> dao.vSetIfPresent("key", "value", Duration.ofNanos(1)))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(values);
        when(values.setIfAbsent("key", "value", Duration.ofSeconds(1))).thenReturn(null);
        assertThatThrownBy(() -> dao.vSetIfAbsent("key", "value", Duration.ofSeconds(1)))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> dao.zIncrementScore("key", "member", Double.NaN))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
