package com.peach.rocket.security;

import com.peach.rocket.autoconfigure.PeachRocketProperties;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import org.springframework.util.StringUtils;
import com.peach.rocket.exception.MqException;

/**
 * 配置MQ键提供者。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/26
 */
public class ConfigMqKeyProvider implements MqKeyProvider {

    private final PeachRocketProperties properties;

    public ConfigMqKeyProvider(PeachRocketProperties properties) {
        this.properties = properties;
    }

    @Override
    public byte[] getKey(String keyId) {
        if (!StringUtils.hasText(keyId)) {
            throw new MqException("MQ encryption key id must not be blank");
        }
        PeachRocketProperties.Security security = properties.getSecurity();
        Map<String, String> keys = security.getKeys();
        String key = keys.get(keyId);
        // 兼容单密钥旧配置：仅允许当前 keyId 使用旧配置，绝不让未知 keyId 隐式解密。
        if (key == null && keyId.equals(security.getKeyId())) {
            key = security.getKey();
        }
        if (!StringUtils.hasText(key)) {
            throw new MqException("Unknown or empty MQ encryption key id");
        }
        try {
            byte[] bytes = security.isBase64Key()
                    ? Base64.getDecoder().decode(key) : key.getBytes(StandardCharsets.UTF_8);
            if (bytes.length != 16 && bytes.length != 24 && bytes.length != 32) {
                throw new MqException("MQ AES key must contain 16, 24 or 32 bytes");
            }
            return bytes;
        } catch (IllegalArgumentException ex) {
            throw new MqException("Invalid MQ encryption key encoding", ex);
        }
    }
}
