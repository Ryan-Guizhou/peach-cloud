package com.peach.gateway.core.ratelimit.service;

import com.peach.gateway.core.security.GatewayTokenContextResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 从当前请求的可信 Token Session 中解析租户 ID。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/06
 */
@Component
@RequiredArgsConstructor
public class RateLimitTenantContextResolver {

    private final GatewayTokenContextResolver tokenContextResolver;

    /**
     * 解析当前请求的租户 ID。
     *
     * @param exchange 当前请求上下文
     * @return 租户 ID；缺失或不完整的 Token Session 返回空信号
     */
    public Mono<String> resolve(ServerWebExchange exchange) {
        return tokenContextResolver.resolve(exchange).map(context -> context.tenantId());
    }
}
