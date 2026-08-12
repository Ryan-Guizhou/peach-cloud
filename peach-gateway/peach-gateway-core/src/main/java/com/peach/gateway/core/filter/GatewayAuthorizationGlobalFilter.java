package com.peach.gateway.core.filter;

import cn.dev33.satoken.exception.DisableServiceException;
import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import cn.dev33.satoken.exception.NotSafeException;
import cn.dev33.satoken.reactor.context.SaReactorSyncHolder;
import cn.dev33.satoken.stp.StpUtil;
import com.peach.gateway.core.config.GatewaySaTokenProperties;
import com.peach.gateway.core.constant.GatewayConstant;
import com.peach.gateway.core.security.GatewaySecurityEndpointMatcher;
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
 * 网关认证过滤器。
 *
 * <p>请求进入下游服务前校验 Sa-Token 登录态，公开端点按配置跳过。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/8/11 14:45
 */
@Slf4j
@Indexed
@Component
public class GatewayAuthorizationGlobalFilter implements GlobalFilter, Ordered {

    private final GatewaySaTokenProperties properties;

    private final GatewaySecurityEndpointMatcher endpointMatcher = new GatewaySecurityEndpointMatcher();

    public GatewayAuthorizationGlobalFilter(GatewaySaTokenProperties properties) {
        this.properties = properties;
    }


    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!properties.isEnabled()) {
            return chain.filter(exchange);
        }
        String path = exchange.getRequest().getURI().getRawPath();
        String method = exchange.getRequest().getMethodValue();
        if (endpointMatcher.matches(properties.getPublicEndpoints(), method, path)) {
            if (properties.isLogPath()) {
                log.info("Gateway authorization skipped, method={}, path={}", method, path);
            }
            return chain.filter(exchange);
        }
        try {
            SaReactorSyncHolder.setContext(exchange);
            StpUtil.checkLogin();
            return chain.filter(exchange)
                    .doFinally(signal -> SaReactorSyncHolder.clearContext());
        } catch (Throwable e) {
            SaReactorSyncHolder.clearContext();
            return handleAuthenticationError(exchange, method, path, e);
        }
    }

    /**
     * 处理认证异常并写入安全错误响应。
     *
     * @param exchange 当前网关交换上下文
     * @param method 请求方法
     * @param path 请求路径
     * @param e 认证异常
     * @return 响应写入完成信号
     */
    private Mono<Void> handleAuthenticationError(ServerWebExchange exchange, String method,
                                                String path, Throwable e) {
        HttpStatus status = resolveStatus(e);
        String requestId = exchange.getRequest().getHeaders().getFirst(GatewayConstant.REQUEST_ID_HEADER);
        log.warn("Gateway authorization rejected, requestId={}, method={}, path={}, status={}, reason={}",
                requestId, method, path, status.value(), e.getClass().getSimpleName());
        return GatewayFilterSupport.writeError(exchange, status, resolveMessage(e));
    }

    /**
     * 将认证异常映射为 HTTP 状态码。
     *
     * @param e 认证异常
     * @return HTTP 状态码
     */
    private HttpStatus resolveStatus(Throwable e) {
        if (e instanceof NotLoginException) {
            return HttpStatus.UNAUTHORIZED;
        }
        if (e instanceof NotPermissionException || e instanceof NotRoleException || e instanceof NotSafeException
                || e instanceof DisableServiceException) {
            return HttpStatus.FORBIDDEN;
        }
        return HttpStatus.UNAUTHORIZED;
    }

    /**
     * 将认证异常映射为安全的客户端错误信息。
     *
     * @param e 认证异常
     * @return 客户端错误信息
     */
    private String resolveMessage(Throwable e) {
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
        if (e instanceof DisableServiceException) {
            return "Account access restricted";
        }
        return "Authentication failed";
    }

    @Override
    public int getOrder() {
        return -200;
    }
}
