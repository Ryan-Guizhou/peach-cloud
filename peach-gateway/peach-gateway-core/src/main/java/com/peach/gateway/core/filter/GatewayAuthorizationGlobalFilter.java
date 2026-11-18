package com.peach.gateway.core.filter;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.exception.DisableServiceException;
import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import cn.dev33.satoken.exception.NotSafeException;
import cn.dev33.satoken.reactor.context.SaReactorSyncHolder;
import cn.dev33.satoken.stp.StpUtil;
import com.peach.common.constant.SaTokenConstant;
import com.peach.gateway.core.config.GatewaySaTokenProperties;
import com.peach.gateway.core.constant.GatewayConstant;
import com.peach.gateway.core.security.GatewaySecurityEndpointMatcher;
import com.peach.gateway.core.security.GatewayTokenContextResolver;
import com.peach.gateway.core.security.GatewayIamDecisionVerifier;
import com.peach.gateway.core.support.GatewayFilterSupport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Indexed;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Locale;

/**
 * 网关可信 SaaS API 资源授权过滤器。
 * <p>公开端点以最小白名单放行；受保护请求先校验登录态，
 * 再以当前 Token 专属 SaaS 快照调用可信 IAM 决策。
 * 决策器缺失、异常或返回空均拒绝，不读取旧用户级缓存兜底。</p>
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

    private final GatewayIamDecisionVerifier decisionVerifier;

    private final GatewayTokenContextResolver tokenContextResolver;

    private final GatewaySecurityEndpointMatcher endpointMatcher = new GatewaySecurityEndpointMatcher();

    public GatewayAuthorizationGlobalFilter(GatewaySaTokenProperties properties,
                                            ObjectProvider<GatewayIamDecisionVerifier> verifierProvider,
                                            GatewayTokenContextResolver tokenContextResolver) {
        this.properties = properties;
        this.decisionVerifier = verifierProvider.getIfAvailable();
        this.tokenContextResolver = tokenContextResolver;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getRawPath();
        String method = exchange.getRequest().getMethod().name();
        // 内部 Same-Token IAM 接口不得经公共网关暴露为任意浏览器的受信服务调用。
        if (isInternalIamPath(path)) {
            return GatewayFilterSupport.writeError(exchange, HttpStatus.FORBIDDEN,
                    "Internal IAM endpoint is not public");
        }
        if (endpointMatcher.matches(properties.getPublicEndpoints(), method, path)) {
            if (properties.isLogPath()) {
                log.info("Gateway authorization skipped, method={}, path={}", method, path);
            }
            return chain.filter(exchange);
        }
        if (!properties.isEnabled()) {
            log.error("Gateway authentication disabled; rejecting protected request, method={}, path={}",
                    method, path);
            return GatewayFilterSupport.writeError(exchange, HttpStatus.SERVICE_UNAVAILABLE,
                    "Authorization service unavailable");
        }
        return Mono.fromCallable(() -> {
                    SaReactorSyncHolder.setContext(exchange);
                    try {
                        StpUtil.checkLogin();
                        return StpUtil.getLoginIdAsString();
                    } finally {
                        SaReactorSyncHolder.clearContext();
                    }
                })
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorResume(exception -> handleAuthenticationError(exchange, method, path, exception)
                        .then(Mono.empty()))
                .flatMap(loginId -> checkApiPermission(loginId, method, path, exchange)
                        .flatMap(allowed -> Boolean.TRUE.equals(allowed)
                                ? chain.filter(exchange)
                                : handlePermissionError(exchange, method, path)));
    }

    /**
     * 校验当前用户是否拥有 API 资源访问权限。
     *
     * @param loginId 当前登录 ID
     * @param method 请求方法
     * @param path 请求路径
     * @return 允许访问时返回 {@code true}
     */
    private Mono<Boolean> checkApiPermission(String loginId, String method, String path,
                                             ServerWebExchange exchange) {
        // 永远不能以旧 Token 自报的授权版本和 Redis Set 存在来证明权限仍然有效。
        // 当前无可信 IAM 验证器时拒绝；#209 负责服务端数据库事实源和可信传播。
        if (!properties.isApiPermissionEnabled() || decisionVerifier == null) {
            return Mono.just(false);
        }
        String resourceCode = buildApiResourceCode(method, path);
        if (isBlank(loginId) || isBlank(resourceCode)) {
            return Mono.just(false);
        }
        return tokenContextResolver.resolve(exchange)
                .flatMap(context -> decisionVerifier.authorize(exchange, context, resourceCode))
                .defaultIfEmpty(false)
                .onErrorResume(exception -> {
                    log.warn("Gateway IAM decision failed, method={}, path={}, reason={}",
                            method, path, exception.getClass().getSimpleName());
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

    private boolean isInternalIamPath(String path) {
        return "/auth/external/iam".equals(path)
                || path.startsWith("/auth/external/iam/")
                || "/api/auth/external/iam".equals(path)
                || path.startsWith("/api/auth/external/iam/");
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
                    + ", authorizationHeaderExists=" + authorizationHeaderExists
                    + ", saTokenDao=" + SaManager.getSaTokenDao().getClass().getName();
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
