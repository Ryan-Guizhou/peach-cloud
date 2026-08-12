package com.peach.gateway.core.filter;

import cn.dev33.satoken.reactor.context.SaReactorSyncHolder;
import cn.dev33.satoken.same.SaSameUtil;
import com.peach.gateway.core.config.GatewaySaTokenProperties;
import com.peach.gateway.core.constant.GatewayConstant;
import com.peach.gateway.core.security.GatewaySecurityEndpointMatcher;
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
 * <p>非公开端点通过网关认证后，由该过滤器向下游业务服务注入 Same-Token，
 * 供业务服务侧 Same-Token 拦截器校验。公开端点与登录校验使用同一套白名单，
 * 不注入 Same-Token，避免未登录公开请求依赖服务间凭证。</p>
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

    private final GatewaySecurityEndpointMatcher endpointMatcher = new GatewaySecurityEndpointMatcher();

    /**
     * 创建网关 Same-Token 传递过滤器。
     *
     * @param properties 网关 Sa-Token 配置
     */
    public GatewaySameTokenGlobalFilter(GatewaySaTokenProperties properties) {
        this.properties = properties;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!properties.isEnabled() || !properties.isInjectSameToken()) {
            return chain.filter(exchange);
        }
        String path = exchange.getRequest().getURI().getRawPath();
        String method = exchange.getRequest().getMethodValue();
        if (endpointMatcher.matches(properties.getPublicEndpoints(), method, path)) {
            if (properties.isLogPath()) {
                log.info("Gateway same-token relay skipped for public endpoint, method={}, path={}", method, path);
            }
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
            String requestId = exchange.getRequest().getHeaders().getFirst(GatewayConstant.REQUEST_ID_HEADER);
            log.warn("Gateway same-token relay failed, requestId={}, method={}, path={}, reason={}",
                    requestId, method, path, e.getClass().getSimpleName());
            return Mono.error(e);
        }
    }

    @Override
    public int getOrder() {
        return -150;
    }
}
