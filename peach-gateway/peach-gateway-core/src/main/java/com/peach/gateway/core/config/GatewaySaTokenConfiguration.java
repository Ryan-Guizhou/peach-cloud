package com.peach.gateway.core.config;

import cn.dev33.satoken.strategy.SaStrategy;
import com.peach.gateway.core.constant.GatewayConstant;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Indexed;

import javax.annotation.PostConstruct;
import java.text.MessageFormat;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 网关 Sa-Token 配置。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/8/11 14:45
 */
@Slf4j
@Indexed
@Configuration
@EnableConfigurationProperties(GatewaySaTokenProperties.class)
public class GatewaySaTokenConfiguration {

    private final GatewaySaTokenProperties properties;

    public GatewaySaTokenConfiguration(GatewaySaTokenProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void rewriteSaTokenStrategy() {
        if (!properties.isEnabled()) {
            log.info("GatewaySaTokenConfiguration is disabled");
            return;
        }
        if (!properties.isTokenStrategyEnabled()) {
            log.info("GatewaySaTokenConfiguration is disabled");
            return;
        }
        SaStrategy.instance.createToken = (loginId, loginType) -> {
            int random = ThreadLocalRandom.current().nextInt(0, 9999);
            String signKey;
            try {
                signKey = MessageFormat.format(GatewayConstant.TOKEN_INCLUDE_USER,
                        System.currentTimeMillis(), random, String.valueOf(loginId));
            } catch (Exception e) {
                log.error("Create Sa-Token token failed", e);
                signKey = MessageFormat.format(GatewayConstant.TOKEN_NOT_INCLUDE_USER,
                        System.currentTimeMillis(), random);
            }
            return DigestUtils.sha256Hex(signKey);
        };
    }
}
