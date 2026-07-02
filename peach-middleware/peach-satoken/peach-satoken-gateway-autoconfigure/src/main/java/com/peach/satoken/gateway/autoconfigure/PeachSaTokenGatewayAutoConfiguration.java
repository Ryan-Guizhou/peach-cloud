package com.peach.satoken.gateway.autoconfigure;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.reactor.filter.SaReactorFilter;
import cn.dev33.satoken.same.SaSameUtil;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.strategy.SaStrategy;
import cn.dev33.satoken.util.SaResult;
import com.peach.satoken.config.PeachSaTokenProperties;
import com.peach.satoken.constant.SatokenConstant;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;

import javax.annotation.PostConstruct;
import java.text.MessageFormat;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@ConditionalOnClass({SaReactorFilter.class, GlobalFilter.class})
@ConditionalOnProperty(prefix = "peach.satoken.gateway", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(PeachSaTokenProperties.class)
public class PeachSaTokenGatewayAutoConfiguration {

    private final PeachSaTokenProperties properties;

    public PeachSaTokenGatewayAutoConfiguration(PeachSaTokenProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void rewriteSaTokenStrategy() {
        if (!properties.getGateway().isTokenStrategyEnabled()) {
            return;
        }
        SaStrategy.instance.createToken = (loginId, loginType) -> {
            int random = ThreadLocalRandom.current().nextInt(0, 9999);
            String signKey;
            try {
                signKey = MessageFormat.format(SatokenConstant.TOKEN_INCLUDE_USER, System.currentTimeMillis(), random, String.valueOf(loginId));
            } catch (Exception e) {
                log.error("createToken error", e);
                signKey = MessageFormat.format(SatokenConstant.TOKEN_NOT_INCLUDE_USER, System.currentTimeMillis(), random);
            }
            return DigestUtils.sha256Hex(signKey);
        };
    }

    @Bean
    public SaReactorFilter peachSaReactorFilter() {
        return new SaReactorFilter()
                .addInclude("/**")
                .setAuth(obj -> {
                    String path = SaHolder.getRequest().getRequestPath();
                    if (properties.getGateway().isLogPath()) {
                        log.info("Gateway Sa-Token check entering path: {}", path);
                    }
                    if (isWhiteList(path)) {
                        log.info("Gateway Sa-Token skip token check, path: {}", path);
                        return;
                    }
                    StpUtil.checkLogin();
                })
                .setError(e -> {
                    log.error("Gateway Sa-Token authentication failed: {}", e.getMessage());
                    return SaResult.error(e.getMessage());
                });
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    @ConditionalOnProperty(prefix = "peach.satoken.gateway", name = "inject-same-token", havingValue = "true", matchIfMissing = true)
    public GlobalFilter peachSameTokenFilter() {
        return (exchange, chain) -> {
            String sameToken = SaSameUtil.getToken();
            ServerHttpRequest request = exchange.getRequest().mutate()
                    .header(SaSameUtil.SAME_TOKEN, sameToken)
                    .build();
            return chain.filter(exchange.mutate().request(request).build());
        };
    }

    private boolean isWhiteList(String path) {
        return properties.getGateway().getWhiteList().stream().anyMatch(path::contains);
    }
}
