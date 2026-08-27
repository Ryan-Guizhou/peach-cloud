package com.peach.satoken.autoconfigure;

import cn.dev33.satoken.strategy.SaStrategy;
import com.peach.common.constant.SaTokenConstant;
import com.peach.common.util.Md5Util;
import com.peach.common.util.PeachSecureRandom;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Indexed;

import jakarta.annotation.PostConstruct;
import java.text.MessageFormat;

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

    /**
     * Override Sa-Token token creation strategy and keep it consistent with gateway.
     */
    @PostConstruct
    public void rewriteSaTokenStrategy() {
        SaStrategy.instance.createToken = (loginId, loginType) -> {
            int random = PeachSecureRandom.get().nextInt(10_000);
            String signKey;
            try {
                signKey = MessageFormat.format(SaTokenConstant.TOKEN_INCLUDE_USER,
                        System.currentTimeMillis(), random, String.valueOf(loginId));
            } catch (Exception e) {
                log.error("Sa-Token token seed creation failed", e);
                signKey = MessageFormat.format(SaTokenConstant.TOKEN_NOT_INCLUDE_USER,
                        System.currentTimeMillis(), random);
            }
            return Md5Util.sha256Hex(signKey);
        };
    }
}
