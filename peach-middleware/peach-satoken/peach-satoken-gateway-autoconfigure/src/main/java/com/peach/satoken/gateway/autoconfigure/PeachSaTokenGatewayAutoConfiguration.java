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


/**
 * Gateway 端 Sa-Token 自动配置。
 *
 * <p>负责网关侧 token 生成策略、认证过滤器以及同 token 注入。</p>

 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/26
 */
@Slf4j
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@ConditionalOnClass({SaReactorFilter.class, GlobalFilter.class})
@ConditionalOnProperty(prefix = "peach.satoken.gateway", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(PeachSaTokenProperties.class)
public class PeachSaTokenGatewayAutoConfiguration {

    private final PeachSaTokenProperties properties;

    /**
     * 创建网关自动配置。
     *
     * @param properties Sa-Token 配置
     */
    public PeachSaTokenGatewayAutoConfiguration(PeachSaTokenProperties properties) {
        this.properties = properties;
    }

    /**
     * 重写网关侧 token 生成策略。
     */
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

    /**
     * 注册网关认证过滤器。
     *
     * @return Sa-Token 响应式过滤器
     */
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

    /**
     * 注册同 token 注入过滤器。
     *
     * @return 全局过滤器
     */
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

    /**
     * 判断路径是否在白名单内。
     *
     * @param path 请求路径
     * @return 是否命中白名单
     */
    private boolean isWhiteList(String path) {
        return properties.getGateway().getWhiteList().stream().anyMatch(path::contains);
    }
}
