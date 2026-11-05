package com.peach.gateway.core.ratelimit.service;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import com.peach.common.constant.SaTokenConstant;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * 从当前 Sa-Token Token Session 解析可信租户上下文。
 *
 * <p>Token Session 按 token 隔离，读取 Sa-Token DAO 的阻塞操作统一调度到 boundedElastic，
 * 不在 Gateway EventLoop 上执行。不会创建缺失的 Token Session，也不接受客户端租户请求头。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/05
 */
@Component
public class RateLimitTenantContextResolver {

    /**
     * 解析当前请求 Token 对应的租户 ID。
     *
     * @return 租户 ID；未登录、无 Token 或 Token Session 未设置租户时返回空信号
     */
    public Mono<String> resolve() {
        String token;
        try {
            token = StpUtil.getTokenValue();
        } catch (RuntimeException exception) {
            return Mono.empty();
        }
        if (token == null || token.isBlank()) {
            return Mono.empty();
        }

        return Mono.fromCallable(() -> StpUtil.getStpLogic().getTokenSessionByToken(token, false))
                .subscribeOn(Schedulers.boundedElastic())
                .map(this::tenantId)
                .filter(value -> value != null && !value.isBlank());
    }

    private String tenantId(SaSession session) {
        if (session == null) {
            return null;
        }
        String tenantId = session.getString(SaTokenConstant.TOKEN_SESSION_TENANT_ID);
        if (tenantId == null) {
            return null;
        }
        String normalized = tenantId.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
