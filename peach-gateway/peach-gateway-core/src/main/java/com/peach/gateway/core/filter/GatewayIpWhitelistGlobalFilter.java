package com.peach.gateway.core.filter;

import com.peach.gateway.core.constant.GatewayConstant;
import com.peach.gateway.core.support.GatewayFilterSupport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Indexed;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 网关 IP 白名单过滤器。
 *
 * <p>白名单由 setting 模块维护并预热到 Redis Set。Redis Set 为空时视为未启用白名单；
 * Redis 不可用时放行请求并记录风险日志，避免缓存故障导致全站不可用。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/8/12 00:00
 */
@Slf4j
@Indexed
@Component
public class GatewayIpWhitelistGlobalFilter implements GlobalFilter, Ordered {

    private final ReactiveStringRedisTemplate redisTemplate;

    public GatewayIpWhitelistGlobalFilter(ObjectProvider<ReactiveStringRedisTemplate> redisTemplateProvider) {
        this.redisTemplate = redisTemplateProvider.getIfAvailable();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String client = GatewayFilterSupport.clientAddress(exchange);
        String method = exchange.getRequest().getMethod().name();
        String path = exchange.getRequest().getURI().getRawPath();
        if (redisTemplate == null || isBlank(client) || "unknown".equals(client)) {
            return chain.filter(exchange);
        }
        return redisTemplate.opsForSet()
                .size(GatewayConstant.RISK_CONTROL_WHITELIST_IP_KEY)
                .defaultIfEmpty(0L)
                .flatMap(size -> {
                    if (size == null || size <= 0) {
                        return chain.filter(exchange);
                    }
                    return redisTemplate.opsForSet()
                            .isMember(GatewayConstant.RISK_CONTROL_WHITELIST_IP_KEY, client)
                            .defaultIfEmpty(false)
                            .flatMap(allowed -> Boolean.TRUE.equals(allowed)
                                    ? chain.filter(exchange)
                                    : reject(exchange, method, path, client));
                })
                .onErrorResume(e -> {
                    log.warn("Gateway IP whitelist check failed, method={}, path={}, reason={}",
                            method, path, e.getClass().getSimpleName());
                    return chain.filter(exchange);
                });
    }

    /**
     * 拒绝未命中白名单的请求。
     *
     * @param exchange 当前网关交换上下文
     * @param method 请求方法
     * @param path 请求路径
     * @param client 客户端地址
     * @return 响应写入完成信号
     */
    private Mono<Void> reject(ServerWebExchange exchange, String method, String path, String client) {
        String requestId = exchange.getRequest().getHeaders().getFirst(GatewayConstant.REQUEST_ID_HEADER);
        log.warn("Gateway IP whitelist rejected request, requestId={}, method={}, path={}, client={}",
                requestId, method, path, client);
        return GatewayFilterSupport.writeError(exchange, HttpStatus.FORBIDDEN,
                "Request blocked by gateway IP whitelist");
    }

    /**
     * 判断字符串是否为空。
     *
     * @param value 待检查字符串
     * @return 为空时返回 {@code true}
     */
    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    @Override
    public int getOrder() {
        return -290;
    }
}
