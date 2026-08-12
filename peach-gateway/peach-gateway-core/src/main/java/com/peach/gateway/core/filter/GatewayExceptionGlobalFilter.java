package com.peach.gateway.core.filter;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import cn.dev33.satoken.exception.NotSafeException;
import cn.dev33.satoken.exception.SameTokenInvalidException;
import com.peach.gateway.core.constant.GatewayConstant;
import com.peach.gateway.core.support.GatewayFilterSupport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Indexed;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * 网关统一异常过滤器。
 *
 * <p>作为网关最后一道异常防线，将异常映射为安全响应，避免将内部异常信息返回给客户端。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/8/11 14:45
 */
@Slf4j
@Indexed
@Component
public class GatewayExceptionGlobalFilter implements GlobalFilter, Ordered {

    /**
     * 包装下游过滤器链并捕获同步或异步异常。
     *
     * @param exchange 当前网关交换上下文
     * @param chain 网关过滤器链
     * @return 过滤完成信号
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        try {
            return chain.filter(exchange)
                    .onErrorResume(e -> handle(exchange, e));
        } catch (Throwable e) {
            return handle(exchange, e);
        }
    }

    /**
     * 处理网关异常并写入统一错误响应。
     *
     * @param exchange 当前网关交换上下文
     * @param e 捕获到的异常
     * @return 响应写入完成信号
     */
    private Mono<Void> handle(ServerWebExchange exchange, Throwable e) {
        HttpStatus status = resolveStatus(e);
        String requestId = ensureRequestId(exchange);
        String method = exchange.getRequest().getMethodValue();
        String path = exchange.getRequest().getURI().getRawPath();
        if (status.is5xxServerError()) {
            log.error("Gateway unhandled exception, requestId={}, method={}, path={}, status={}, reason={}",
                    requestId, method, path, status.value(), e.getClass().getSimpleName());
        } else {
            log.warn("Gateway request rejected, requestId={}, method={}, path={}, status={}, reason={}",
                    requestId, method, path, status.value(), e.getClass().getSimpleName());
        }
        return GatewayFilterSupport.writeError(exchange, status, resolveMessage(e, status));
    }

    /**
     * 确保异常响应中存在请求 ID。
     *
     * @param exchange 当前网关交换上下文
     * @return 请求 ID
     */
    private String ensureRequestId(ServerWebExchange exchange) {
        String requestId = exchange.getRequest().getHeaders().getFirst(GatewayConstant.REQUEST_ID_HEADER);
        if (requestId == null || requestId.trim().isEmpty()) {
            requestId = UUID.randomUUID().toString().replace("-", "");
            exchange.getResponse().getHeaders().set(GatewayConstant.REQUEST_ID_HEADER, requestId);
        }
        return requestId;
    }

    /**
     * 将异常映射为 HTTP 状态码。
     *
     * @param e 捕获到的异常
     * @return HTTP 状态码
     */
    private HttpStatus resolveStatus(Throwable e) {
        if (e instanceof NotLoginException) {
            return HttpStatus.UNAUTHORIZED;
        }
        if (e instanceof NotPermissionException || e instanceof NotRoleException || e instanceof NotSafeException
                || e instanceof SameTokenInvalidException) {
            return HttpStatus.FORBIDDEN;
        }
        if (e instanceof ResponseStatusException) {
            return ((ResponseStatusException) e).getStatus();
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    /**
     * 将异常映射为安全的客户端错误信息。
     *
     * @param e 捕获到的异常
     * @param status 已解析的 HTTP 状态码
     * @return 客户端错误信息
     */
    private String resolveMessage(Throwable e, HttpStatus status) {
        if (e instanceof NotLoginException) {
            return "Authentication required";
        }
        if (e instanceof NotPermissionException) {
            return "Permission denied";
        }
        if (e instanceof NotRoleException) {
            return "Role permission denied";
        }
        if (e instanceof NotSafeException) {
            return "Secondary authentication required";
        }
        if (e instanceof SameTokenInvalidException) {
            return "Service credential invalid";
        }
        if (HttpStatus.NOT_FOUND.equals(status)) {
            return "Resource not found";
        }
        if (status.is4xxClientError()) {
            return "Invalid request";
        }
        return "Gateway service unavailable";
    }


    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
