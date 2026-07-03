package com.peach.message.config;

import cn.dev33.satoken.same.SaSameUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Indexed;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Message 服务对 WebSocket 与普通 HTTP 请求补充 Same-Token 校验。
 */
@Slf4j
@Indexed
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnProperty(prefix = "peach.message.same-token", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MessageSameTokenWebFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (isWhiteList(path)) {
            return chain.filter(exchange);
        }
        String sameToken = exchange.getRequest().getHeaders().getFirst(SaSameUtil.SAME_TOKEN);
        if (StringUtils.isBlank(sameToken)) {
            sameToken = exchange.getRequest().getQueryParams().getFirst(SaSameUtil.SAME_TOKEN);
        }
        try {
            SaSameUtil.checkToken(sameToken);
            return chain.filter(exchange);
        } catch (Exception e) {
            log.warn("Message service SameToken check failed, path={}, msg={}", path, e.getMessage());
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    private boolean isWhiteList(String path) {
        return StringUtils.contains(path, "health")
                || StringUtils.contains(path, "/actuator")
                || StringUtils.contains(path, "/doc.html")
                || StringUtils.contains(path, "/webjars")
                || StringUtils.contains(path, "/swagger-resources")
                || StringUtils.contains(path, "/v3/api-docs")
                || StringUtils.contains(path, "/v2/api-docs");
    }
}
