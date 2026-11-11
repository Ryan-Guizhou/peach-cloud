package com.peach.satoken.autoconfigure;

import cn.dev33.satoken.strategy.SaStrategy;
import com.peach.common.util.PeachSecureRandom;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Indexed;

import jakarta.annotation.PostConstruct;
import java.util.HexFormat;

/**
 * PeachSa令牌Strategy自动配置。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/8/13 16:30
 */
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
            byte[] tokenBytes = new byte[32];
            PeachSecureRandom.get().nextBytes(tokenBytes);
            return HexFormat.of().formatHex(tokenBytes);
        };
    }
}
