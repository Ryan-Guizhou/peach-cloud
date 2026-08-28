package com.peach.gateway.core.filter;

import com.peach.gateway.core.constant.GatewayConstant;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Indexed;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * 网关请求Global过滤器。
 * <p>网关统一生成可信请求 ID，并传递给下游服务和响应头。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/8/11 14:45
 */
@Indexed
@Component
public class GatewayRequestGlobalFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String requestId = createRequestId();
        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.remove(GatewayConstant.REQUEST_ID_HEADER);
                    headers.add(GatewayConstant.REQUEST_ID_HEADER, requestId);
                }).build();
        exchange.getResponse().getHeaders().set(GatewayConstant.REQUEST_ID_HEADER, requestId);
        return chain.filter(exchange.mutate().request(request).build());
    }


    @Override
    public int getOrder() {
        return -300;
    }

    /**
     * 创建不带连字符的请求 ID。
     *
     * @return 请求 ID
     */
    private String createRequestId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
