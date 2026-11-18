package com.peach.gateway.core.security;

import cn.dev33.satoken.reactor.context.SaReactorSyncHolder;
import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import com.peach.common.constant.SaTokenConstant;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Map;

/**
 * 从当前请求 Token Session 解析可信租户上下文，并在同一请求内复用解析结果。
 *
 * <p>Sa-Token DAO 通过 Lettuce 阻塞 API 读取 Token Session。读取和上下文绑定都在 boundedElastic 上完成，
 * 避免在 Netty EventLoop 中等待 Redis。解析结果存放在当前 ServerWebExchange 属性中，供认证和限流复用。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/06
 */
@Component
public class GatewayTokenContextResolver {

    private static final String EXCHANGE_ATTRIBUTE = "peach.gateway.token-context";

    /**
     * 解析当前请求凭证对应的租户上下文。
     *
     * @param exchange 当前请求上下文
     * @return 完整租户上下文；Token Session 缺失或上下文不完整时返回空信号
     */
    public Mono<GatewayTokenContext> resolve(ServerWebExchange exchange) {
        return Mono.defer(() -> {
            GatewayTokenContext cached = exchange.getAttribute(EXCHANGE_ATTRIBUTE);
            if (cached != null) {
                return Mono.just(cached);
            }

            return Mono.fromCallable(() -> {
                        SaReactorSyncHolder.setContext(exchange);
                        try {
                            String token = StpUtil.getTokenValue();
                            if (token == null || token.isBlank()) {
                                return null;
                            }
                            return StpUtil.getStpLogic().getTokenSessionByToken(token, false);
                        } finally {
                            SaReactorSyncHolder.clearContext();
                        }
                    })
                    .subscribeOn(Schedulers.boundedElastic())
                    .flatMap(session -> session == null ? Mono.empty() : toContext(session))
                    .doOnNext(context -> exchange.getAttributes().put(EXCHANGE_ATTRIBUTE, context));
        });
    }

    /**
     * 只接受一次性写入的新版可信 SaaS Token Session 快照，旧 FISCAL/ORG 单字段一律拒绝。
     *
     * <p>不存在机构也可合法登录。无法解析成员或授权版本时返回空授权上下文。</p>
     *
     * @param session 已关联当前 Token 的 Sa-Token Session
     * @return SaaS 物理身份快照；非法或缺失时为空信号
     */
    Mono<GatewayTokenContext> toContext(SaSession session) {
        if (session == null) {
            return Mono.empty();
        }
        Object stored = session.get(SaTokenConstant.TOKEN_SESSION_SAAS_CONTEXT);
        if (!(stored instanceof Map<?, ?> values)
                || values.containsKey("year") || values.containsKey("fiscal")) {
            return Mono.empty();
        }
        String user = requiredString(values.get("userRefId"));
        String tenant = requiredString(values.get("tenantRefId"));
        String member = requiredString(values.get("memberRefId"));
        String app = requiredString(values.get("appRefId"));
        String versionText = requiredString(values.get("memberVersion"));
        String authText = requiredString(values.get("authVersion"));
        if (user == null || tenant == null || member == null || app == null
                || versionText == null || authText == null) {
            return Mono.empty();
        }
        long version;
        long authVersion;
        try {
            version = Long.parseLong(versionText);
            authVersion = Long.parseLong(authText);
        } catch (NumberFormatException exception) {
            return Mono.empty();
        }
        if (version < 0 || authVersion < 0) {
            return Mono.empty();
        }
        Object orgValue = values.get("orgRefId");
        String org = orgValue == null ? null : requiredString(orgValue);
        if (orgValue != null && org == null) {
            return Mono.empty();
        }
        return Mono.just(new GatewayTokenContext(
                user, tenant, member, app, version, authVersion, org));
    }

    private String requiredString(Object value) {
        if (!(value instanceof String text) || text.isBlank() || text.indexOf(':') >= 0) {
            return null;
        }
        return text.trim();
    }
}
