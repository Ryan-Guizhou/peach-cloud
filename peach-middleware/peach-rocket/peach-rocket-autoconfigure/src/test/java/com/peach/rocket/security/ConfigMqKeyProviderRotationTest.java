package com.peach.rocket.security;

import com.peach.rocket.autoconfigure.PeachRocketProperties;
import com.peach.rocket.exception.MqException;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 配置密钥轮换及未知 keyId 拒绝回归。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/13
 */
class ConfigMqKeyProviderRotationTest {

    @Test
    void shouldResolveEachKeyByIdAndRejectUnknownIds() {
        PeachRocketProperties props = new PeachRocketProperties();
        props.getSecurity().setKeyId("new");
        props.getSecurity().setKeys(Map.of(
                "old", "0123456789abcdef",
                "new", "abcdef0123456789"));
        ConfigMqKeyProvider provider = new ConfigMqKeyProvider(props);

        assertArrayEquals("0123456789abcdef".getBytes(java.nio.charset.StandardCharsets.UTF_8),
                provider.getKey("old"));
        assertArrayEquals("abcdef0123456789".getBytes(java.nio.charset.StandardCharsets.UTF_8),
                provider.getKey("new"));
        assertThrows(MqException.class, () -> provider.getKey("missing"));
    }

    @Test
    void shouldPreserveSingleKeyCompatibilityOnlyForCurrentId() {
        PeachRocketProperties props = new PeachRocketProperties();
        props.getSecurity().setKeyId("active");
        props.getSecurity().setKey("0123456789abcdef");
        ConfigMqKeyProvider provider = new ConfigMqKeyProvider(props);

        provider.getKey("active");
        assertThrows(MqException.class, () -> provider.getKey("inactive"));
    }
}
