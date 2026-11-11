package com.peach.redis.common.tool;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 验证关键 Redis 写入/键操作失败不会被静默解释为 false 或成功。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/11
 */
class RedisDaoFailureSemanticsTest {

    @Test
    @SuppressWarnings("unchecked")
    void shouldRejectMissingReadAndWriteConfirmation() {
        RedisTemplate<Object, Object> redis = mock(RedisTemplate.class);
        RedisDaoImpl dao = new RedisDaoImpl(redis);
        when(redis.hasKey("known")).thenReturn(null);
        when(redis.delete("known")).thenReturn(null);
        when(redis.delete(any(Set.class))).thenReturn(null);

        assertThatThrownBy(() -> dao.existsKey("known"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("EXISTS");
        assertThatThrownBy(() -> dao.delete("known"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("DEL");
        assertThatThrownBy(() -> dao.delete(Set.of("a", "b")))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("DEL");
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldPropagateConnectionFailureFromWritesAndPubSub() {
        RedisTemplate<Object, Object> redis = mock(RedisTemplate.class);
        ValueOperations<Object, Object> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        RedisConnectionFailureException outage = new RedisConnectionFailureException("offline");
        doThrow(outage).when(values).set("key", "value");
        doThrow(outage).when(redis).convertAndSend(anyString(), anyString());

        RedisDaoImpl dao = new RedisDaoImpl(redis);
        assertThatThrownBy(() -> dao.vSet("key", "value")).isSameAs(outage);
        assertThatThrownBy(() -> dao.convertAndSend("events", "payload")).isSameAs(outage);
        assertThatThrownBy(() -> dao.vSet("key", "value", Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
