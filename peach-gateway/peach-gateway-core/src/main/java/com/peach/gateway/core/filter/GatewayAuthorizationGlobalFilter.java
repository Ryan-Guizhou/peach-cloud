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
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Indexed;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Locale;

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

    private static final String PATH_SEPARATOR = "/";

    private final GatewaySaTokenProperties properties;

    private final ReactiveStringRedisTemplate redisTemplate;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    private final GatewaySecurityEndpointMatcher endpointMatcher = new GatewaySecurityEndpointMatcher();

    public GatewayAuthorizationGlobalFilter(GatewaySaTokenProperties properties,
                                            ObjectProvider<ReactiveStringRedisTemplate> redisTemplateProvider) {
        this.properties = properties;
        this.redisTemplate = redisTemplateProvider.getIfAvailable();
    }


    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!properties.isEnabled()) {
            return chain.filter(exchange);
        }
        String path = exchange.getRequest().getURI().getRawPath();
        String method = exchange.getRequest().getMethod().name();
        if (endpointMatcher.matches(properties.getPublicEndpoints(), method, path)) {
            if (properties.isLogPath()) {
                log.info("Gateway authorization skipped, method={}, path={}", method, path);
            }
            return chain.filter(exchange);
        }
        try {
            SaReactorSyncHolder.setContext(exchange);
            StpUtil.checkLogin();
            String loginId = StpUtil.getLoginIdAsString();
            return checkApiPermission(loginId, method, path)
                    .flatMap(allowed -> Boolean.TRUE.equals(allowed)
                            ? chain.filter(exchange)
                            : handlePermissionError(exchange, method, path))
                    .doFinally(signal -> SaReactorSyncHolder.clearContext());
        } catch (Exception e) {
            SaReactorSyncHolder.clearContext();
            return handleAuthenticationError(exchange, method, path, e);
        }
    }

    /**
     * 校验当前用户是否拥有 API 资源访问权限。
     *
     * @param loginId 当前登录 ID
     * @param method 请求方法
     * @param path 请求路径
     * @return 允许访问时返回 {@code true}
     */
    private Mono<Boolean> checkApiPermission(String loginId, String method, String path) {
        if (!properties.isApiPermissionEnabled() || redisTemplate == null) {
            return Mono.just(true);
        }
        String resourceCode = buildApiResourceCode(method, path);
        if (isBlank(loginId) || isBlank(resourceCode)) {
            return Mono.just(false);
        }
        String key = GatewayConstant.USER_API_RESOURCE_CACHE_PREFIX + loginId;
        return redisTemplate.opsForSet()
                .isMember(key, resourceCode)
                .defaultIfEmpty(false)
                .flatMap(exactMatched -> Boolean.TRUE.equals(exactMatched)
                        ? Mono.just(true)
                        : redisTemplate.opsForSet()
                        .members(key)
                        .any(candidate -> apiResourceMatches(candidate, method, path))
                        .defaultIfEmpty(false))
                .onErrorResume(e -> {
                    log.warn("Gateway API permission check failed, method={}, path={}, reason={}",
                            method, path, e.getClass().getSimpleName());
                    return Mono.just(false);
                });
    }

    /**
     * 构建 API 资源编码。
     *
     * @param method 请求方法
     * @param path 请求路径
     * @return API 资源编码
     */
    private String buildApiResourceCode(String method, String path) {
        if (isBlank(method) || isBlank(path)) {
            return "";
        }
        String normalizedPath = path.trim();
        if (!normalizedPath.startsWith(PATH_SEPARATOR)) {
            normalizedPath = PATH_SEPARATOR + normalizedPath;
        }
        return method.trim().toUpperCase(Locale.ENGLISH) + ":" + normalizedPath;
    }

    /**
     * 判断已授权 API 资源编码是否匹配当前请求。
     *
     * @param resourceCode 已授权 API 资源编码
     * @param method 当前请求方法
     * @param path 当前请求路径
     * @return 匹配时返回 {@code true}
     */
    private boolean apiResourceMatches(String resourceCode, String method, String path) {
        if (isBlank(resourceCode) || isBlank(method) || isBlank(path)) {
            return false;
        }
        int separator = resourceCode.indexOf(':');
        if (separator <= 0 || separator >= resourceCode.length() - 1) {
            return false;
        }
        String resourceMethod = resourceCode.substring(0, separator).trim().toUpperCase(Locale.ENGLISH);
        String resourcePath = resourceCode.substring(separator + 1).trim();
        if (!"ANY".equals(resourceMethod) && !resourceMethod.equals(method.trim().toUpperCase(Locale.ENGLISH))) {
            return false;
        }
        String normalizedPath = path.trim();
        if (!normalizedPath.startsWith(PATH_SEPARATOR)) {
            normalizedPath = PATH_SEPARATOR + normalizedPath;
        }
        if (!resourcePath.startsWith(PATH_SEPARATOR)) {
            resourcePath = PATH_SEPARATOR + resourcePath;
        }
        return pathMatcher.match(resourcePath, normalizedPath);
    }

    /**
     * 处理 API 权限不足响应。
     *
     * @param exchange 当前网关交换上下文
     * @param method 请求方法
     * @param path 请求路径
     * @return 响应写入完成信号
     */
    private Mono<Void> handlePermissionError(ServerWebExchange exchange, String method, String path) {
        String requestId = exchange.getRequest().getHeaders().getFirst(GatewayConstant.REQUEST_ID_HEADER);
        log.warn("Gateway API permission rejected, requestId={}, method={}, path={}",
                requestId, method, path);
        return GatewayFilterSupport.writeError(exchange, HttpStatus.FORBIDDEN, "Permission denied");
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
        log.warn("Gateway authorization rejected, requestId={}, method={}, path={}, status={}, reason={}, detail={}",
                requestId, method, path, status.value(), e.getClass().getSimpleName(),
                resolveAuthenticationRejectDetail(exchange, e));
        return GatewayFilterSupport.writeError(exchange, status, resolveMessage(e));
    }

    private String resolveAuthenticationRejectDetail(ServerWebExchange exchange, Throwable e) {
        if (e instanceof NotLoginException notLoginException) {
            String tokenName = StpUtil.getTokenName();
            boolean configuredHeaderExists = exchange.getRequest().getHeaders().containsKey(tokenName);
            boolean authorizationHeaderExists = exchange.getRequest().getHeaders().containsKey("Authorization");
            return "type=" + notLoginException.getType()
                    + ", code=" + notLoginException.getCode()
                    + ", tokenName=" + tokenName
                    + ", configuredHeaderExists=" + configuredHeaderExists
                    + ", authorizationHeaderExists=" + authorizationHeaderExists;
        }
        return e.getClass().getSimpleName();
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
        return -200;
    }
}
