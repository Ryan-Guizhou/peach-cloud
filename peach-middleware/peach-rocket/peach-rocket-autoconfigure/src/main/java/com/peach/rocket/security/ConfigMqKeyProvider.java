package com.peach.rocket.security;

import com.peach.rocket.autoconfigure.PeachRocketProperties;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 基于配置的 MQ 密钥提供者。
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/26
 */
public class ConfigMqKeyProvider implements MqKeyProvider {

    private final PeachRocketProperties properties;

    public ConfigMqKeyProvider(PeachRocketProperties properties) {
        this.properties = properties;
    }

    @Override
    public byte[] getKey(String keyId) {
        String key = properties.getSecurity().getKey();
        return properties.getSecurity().isBase64Key() ? Base64.getDecoder().decode(key) : key.getBytes(StandardCharsets.UTF_8);
    }
}
