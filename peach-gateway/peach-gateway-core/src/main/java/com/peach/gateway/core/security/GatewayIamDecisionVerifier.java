package com.peach.gateway.core.security;

import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 受信 IAM 服务端 API 授权决策扩展点。
 *
 * <p>每次访问必须以服务端数据库/受信 RPC 为事实源验证 Token 对应的成员、
 * 租户、应用、年度、成员版本和 AUTH_VERSION，并核验当前 API 的有效动作授权。
 * 不得仅凭 Token 内版本值、旧用户级 Redis 集合或浏览器自报权限放行；
 * 故障、超时、空信号或不一致均须拒绝。接口禁止做 Netty EventLoop 阻塞 I/O。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/18 12:30
 */
@FunctionalInterface
public interface GatewayIamDecisionVerifier {

    /**
     * 实时核验指定 API 的受保护授权。
     *
     * @param exchange 当前请求的不可变服务端上下文
     * @param context Sa-Token 专属 SaaS 身份快照（不是最新数据库授权证明）
     * @param apiResourceCode 由网关构造的 METHOD:PATH 动作标识
     * @return true 仅代表已经完成可信后端全链路授权判断
     */
    Mono<Boolean> authorize(ServerWebExchange exchange,
                            GatewayTokenContext context, String apiResourceCode);
}
