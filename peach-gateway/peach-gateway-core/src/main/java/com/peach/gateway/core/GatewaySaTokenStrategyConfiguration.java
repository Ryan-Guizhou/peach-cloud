package com.peach.gateway.core;

import cn.dev33.satoken.strategy.SaStrategy;
import com.peach.gateway.core.config.GatewaySaTokenProperties;
import com.peach.gateway.core.constant.GatewayConstant;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Indexed;

import javax.annotation.PostConstruct;
import java.text.MessageFormat;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 网关侧 Sa-Token token 策略配置。
 *
 * <p>该配置属于网关服务内部模块，由 {@code peach-gateway-launch} 的组件扫描加载。
 * 它只处理网关本地 Sa-Token 策略扩展，不依赖业务服务侧 {@code peach-satoken}。
 * 当 {@link GatewaySaTokenProperties#isTokenStrategyEnabled()} 开启时，覆盖 Sa-Token token
 * 生成策略，确保网关与业务服务使用同一套 Sa-Token Redis 数据时能稳定识别登录态。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/8/11 14:45
 */
@Slf4j
@Indexed
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(SaStrategy.class)
public class GatewaySaTokenStrategyConfiguration {

    private final GatewaySaTokenProperties properties;

    /**
     * 创建网关侧 Sa-Token token 策略配置。
     *
     * @param properties 网关 Sa-Token 配置
     */
    public GatewaySaTokenStrategyConfiguration(GatewaySaTokenProperties properties) {
        this.properties = properties;
    }

    /**
     * 按配置覆盖 Sa-Token token 生成策略。
     */
    @PostConstruct
    public void rewriteSaTokenStrategy() {
        if (!properties.isEnabled()) {
            log.info("Gateway Sa-Token strategy configuration skipped, reason=authentication-disabled");
            return;
        }
        if (!properties.isTokenStrategyEnabled()) {
            log.info("Gateway Sa-Token strategy configuration skipped, reason=token-strategy-disabled");
            return;
        }
        SaStrategy.instance.createToken = (loginId, loginType) -> {
            int random = ThreadLocalRandom.current().nextInt(0, 9999);
            String signKey;
            try {
                signKey = MessageFormat.format(GatewayConstant.TOKEN_INCLUDE_USER,
                        System.currentTimeMillis(), random, String.valueOf(loginId));
            } catch (Exception e) {
                log.error("Gateway Sa-Token token seed creation failed", e);
                signKey = MessageFormat.format(GatewayConstant.TOKEN_NOT_INCLUDE_USER,
                        System.currentTimeMillis(), random);
            }
            return DigestUtils.sha256Hex(signKey);
        };
    }
}
