package com.peach.gateway.core.security;

import cn.dev33.satoken.reactor.context.SaReactorSyncHolder;
import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import com.peach.common.constant.SaTokenConstant;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

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
     * 将 Sa-Token Session 转换为完整业务上下文。
     *
     * @param session Token Session
     * @return 完整上下文；租户或机构信息缺失时返回空信号
     */
    Mono<GatewayTokenContext> toContext(SaSession session) {
        return Mono.justOrEmpty(session)
                .flatMap(value -> {
                    String tenantId = normalize(value.getString(SaTokenConstant.TOKEN_SESSION_TENANT_ID));
                    String orgId = normalize(value.getString(SaTokenConstant.TOKEN_SESSION_ORG_ID));
                    String fiscal = normalize(value.getString(SaTokenConstant.TOKEN_SESSION_FISCAL));
                    if (tenantId == null || orgId == null) {
                        return Mono.empty();
                    }
                    return Mono.just(new GatewayTokenContext(tenantId, orgId, fiscal));
                });
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
