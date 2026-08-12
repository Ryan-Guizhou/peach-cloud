package com.peach.gateway.core.filter;

import com.peach.gateway.core.config.GatewayRiskControlProperties;
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
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * 网关基础风控过滤器。
 *
 * <p>在请求进入下游服务前拦截异常请求形态和显式黑名单流量；公开端点按网关安全配置跳过。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/8/11 14:45
 */
@Slf4j
@Indexed
@Component
public class GatewayRiskControlGlobalFilter implements GlobalFilter, Ordered {

    private final GatewaySaTokenProperties saTokenProperties;

    private final GatewayRiskControlProperties riskControlProperties;

    private final ReactiveStringRedisTemplate redisTemplate;

    private final GatewaySecurityEndpointMatcher endpointMatcher = new GatewaySecurityEndpointMatcher();

    public GatewayRiskControlGlobalFilter(GatewaySaTokenProperties saTokenProperties,
                                          GatewayRiskControlProperties riskControlProperties,
                                          ObjectProvider<ReactiveStringRedisTemplate> redisTemplateProvider) {
        this.saTokenProperties = saTokenProperties;
        this.riskControlProperties = riskControlProperties;
        this.redisTemplate = redisTemplateProvider.getIfAvailable();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getRawPath();
        String method = exchange.getRequest().getMethodValue();
        if (!riskControlProperties.isEnabled() || endpointMatcher.matches(saTokenProperties.getPublicEndpoints(), method, path)) {
            return chain.filter(exchange);
        }
        String client = GatewayFilterSupport.remoteAddress(exchange);
        String userAgent = exchange.getRequest().getHeaders().getFirst("User-Agent");
        String reason = rejectReason(exchange, path, method, client, userAgent);
        if (reason != null) {
            return reject(exchange, method, path, client, reason);
        }
        return isRedisBlockedIp(client)
                .flatMap(blocked -> Boolean.TRUE.equals(blocked)
                        ? reject(exchange, method, path, client, "redis-blocked-ip")
                        : chain.filter(exchange));
    }

    /**
     * 解析请求触发风控拒绝的原因。
     *
     * @param exchange 当前网关交换上下文
     * @param path 请求路径
     * @param method 请求方法
     * @param client 客户端地址
     * @param userAgent User-Agent 请求头
     * @return 拒绝原因；未触发拒绝时返回 {@code null}
     */
    private String rejectReason(ServerWebExchange exchange, String path, String method,
                                String client, String userAgent) {
        if (path == null || path.length() > riskControlProperties.getMaxUriLength()) {
            return "uri-length";
        }
        if (exchange.getRequest().getHeaders().size() > riskControlProperties.getMaxHeaderCount()) {
            return "header-count";
        }
        if ("TRACE".equalsIgnoreCase(method) || "CONNECT".equalsIgnoreCase(method)) {
            return "unsupported-method";
        }
        if (isListed(riskControlProperties.getBlockedIps(), client)) {
            return "blocked-ip";
        }
        if (isListed(riskControlProperties.getBlockedUserAgents(), userAgent)) {
            return "blocked-user-agent";
        }
        return null;
    }

    /**
     * 检查客户端地址是否命中 Redis 动态黑名单。
     *
     * @param client 客户端地址
     * @return 命中结果
     */
    private Mono<Boolean> isRedisBlockedIp(String client) {
        if (redisTemplate == null || isBlank(client) || "unknown".equals(client) || isBlank(GatewayConstant.RISK_CONTROL_BLOCKED_IP_KEY)) {
            return Mono.just(false);
        }
        return redisTemplate.opsForSet()
                .isMember(GatewayConstant.RISK_CONTROL_BLOCKED_IP_KEY, client)
                .defaultIfEmpty(false)
                .onErrorResume(e -> {
                    log.warn("Gateway risk-control Redis blocklist check failed, reason={}",
                            e.getClass().getSimpleName());
                    return Mono.just(false);
                });
    }

    /**
     * 记录风控拒绝日志并写入统一错误响应。
     *
     * @param exchange 当前网关交换上下文
     * @param method 请求方法
     * @param path 请求路径
     * @param client 客户端地址
     * @param reason 拒绝原因
     * @return 响应写入完成信号
     */
    private Mono<Void> reject(ServerWebExchange exchange, String method, String path,
                              String client, String reason) {
        log.warn("Gateway risk-control rejected request, method={}, path={}, client={}, reason={}",
                method, path, client, reason);
        return GatewayFilterSupport.writeError(exchange, HttpStatus.FORBIDDEN,
                "Request blocked by gateway security policy");
    }

    /**
     * 判断指定值是否存在于逗号分隔配置中。
     *
     * @param configured 逗号分隔配置值
     * @param value 待匹配值
     * @return 命中时返回 {@code true}
     */
    private boolean isListed(String configured, String value) {
        if (isBlank(value) || isBlank(configured)) {
            return false;
        }
        List<String> values = Arrays.stream(configured.split(","))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .map(item -> item.toLowerCase(Locale.ENGLISH))
                .collect(Collectors.toList());
        return values.contains(value.toLowerCase(Locale.ENGLISH));
    }

    /**
     * 判断字符串是否为空。
     *
     * @param value 待检查字符串
     * @return 为空时返回 {@code true}
     */
    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    @Override
    public int getOrder() {
        return -250;
    }
}
