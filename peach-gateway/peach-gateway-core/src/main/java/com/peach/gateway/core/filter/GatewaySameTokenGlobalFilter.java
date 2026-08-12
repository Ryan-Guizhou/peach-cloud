package com.peach.gateway.core.filter;

import cn.dev33.satoken.same.SaSameUtil;
import cn.dev33.satoken.reactor.context.SaReactorSyncHolder;
import com.peach.gateway.core.config.GatewaySaTokenProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Indexed;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 网关 Same-Token 传递过滤器。
 *
 * <p>为需要服务间认证的下游请求注入 Sa-Token Same-Token。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/8/11 14:45
 */
@Slf4j
@Indexed
@Component
public class GatewaySameTokenGlobalFilter implements GlobalFilter, Ordered {

    private final GatewaySaTokenProperties properties;

    public GatewaySameTokenGlobalFilter(GatewaySaTokenProperties properties) {
        this.properties = properties;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!properties.isEnabled() || !properties.isInjectSameToken()) {
            return chain.filter(exchange);
        }
        try {
            SaReactorSyncHolder.setContext(exchange);
            String sameToken = SaSameUtil.getToken();
            ServerHttpRequest request = exchange.getRequest().mutate()
                    .header(SaSameUtil.SAME_TOKEN, sameToken)
                    .build();
            return chain.filter(exchange.mutate().request(request).build())
                    .doFinally(signal -> SaReactorSyncHolder.clearContext());
        } catch (Throwable e) {
            SaReactorSyncHolder.clearContext();
            log.warn("Gateway same-token relay failed, reason={}", e.getClass().getSimpleName());
            return Mono.error(e);
        }
    }

    @Override
    public int getOrder() {
        return -150;
    }
}
