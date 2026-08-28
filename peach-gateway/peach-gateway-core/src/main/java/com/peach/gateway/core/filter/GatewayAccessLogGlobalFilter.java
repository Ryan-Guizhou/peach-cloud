package com.peach.gateway.core.filter;

import com.peach.gateway.core.constant.GatewayConstant;
import com.peach.gateway.core.support.GatewayFilterSupport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Indexed;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 网关Access日志Global过滤器。
 * <p>仅记录请求方法、路径、状态码、耗时、客户端地址和请求 ID，不记录 query、body、token。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/8/11 14:45
 */
@Slf4j
@Indexed
@Component
public class GatewayAccessLogGlobalFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long startNanos = System.nanoTime();
        return chain.filter(exchange)
                .doFinally(signal -> {
                    long durationMillis = (System.nanoTime() - startNanos) / 1000000L;
                    int status = exchange.getResponse().getStatusCode() == null
                            ? HttpStatus.INTERNAL_SERVER_ERROR.value()
                            : exchange.getResponse().getStatusCode().value();
                    String requestId = exchange.getRequest().getHeaders()
                            .getFirst(GatewayConstant.REQUEST_ID_HEADER);
                    String path = exchange.getRequest().getURI().getPath();
                    String method = exchange.getRequest().getMethod().name();
                    String client = GatewayFilterSupport.clientAddress(exchange);
                    if (status >= 500) {
                        log.warn("Gateway access completed, requestId={}, method={}, path={}, status={}, durationMs={}, client={}",
                                requestId, method, path, status, durationMillis, client);
                    } else {
                        log.info("Gateway access completed, requestId={}, method={}, path={}, status={}, durationMs={}, client={}",
                                requestId, method, path, status, durationMillis, client);
                    }
                });
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
