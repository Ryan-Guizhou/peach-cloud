package com.peach.satoken.autoconfigure;

import cn.dev33.satoken.strategy.SaStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Indexed;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.MessageFormat;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Sa-Token token strategy auto configuration.
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/8/13 16:30
 */
@Slf4j
@Indexed
@AutoConfiguration
@ConditionalOnClass(SaStrategy.class)
@ConditionalOnProperty(prefix = "peach.satoken.token-strategy", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class PeachSaTokenStrategyAutoConfiguration {

    private static final String TOKEN_INCLUDE_USER = "{0}-{1}-{2}";

    private static final String TOKEN_NOT_INCLUDE_USER = "{0}-{1}";

    /**
     * Override Sa-Token token creation strategy and keep it consistent with gateway.
     */
    @PostConstruct
    public void rewriteSaTokenStrategy() {
        SaStrategy.instance.createToken = (loginId, loginType) -> {
            int random = ThreadLocalRandom.current().nextInt(0, 9999);
            String signKey;
            try {
                signKey = MessageFormat.format(TOKEN_INCLUDE_USER,
                        System.currentTimeMillis(), random, String.valueOf(loginId));
            } catch (Exception e) {
                log.error("Sa-Token token seed creation failed", e);
                signKey = MessageFormat.format(TOKEN_NOT_INCLUDE_USER,
                        System.currentTimeMillis(), random);
            }
            return sha256Hex(signKey);
        };
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte item : bytes) {
                String hex = Integer.toHexString(item & 0xff);
                if (hex.length() == 1) {
                    builder.append('0');
                }
                builder.append(hex);
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is not available", e);
        }
    }
}
