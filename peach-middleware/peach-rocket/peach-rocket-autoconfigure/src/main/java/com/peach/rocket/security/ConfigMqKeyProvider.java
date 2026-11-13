package com.peach.rocket.security;

import com.peach.rocket.autoconfigure.PeachRocketProperties;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import org.springframework.util.StringUtils;
import com.peach.rocket.exception.MqException;

/**
 * 按 keyId 读取指定 AES 密钥，禁止回退到不区分密钥标识的配置。
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
