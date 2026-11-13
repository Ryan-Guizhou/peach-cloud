package com.peach.rocket.outbox;

import java.util.Objects;

/**
 * Outbox 单批认领的发送上下文。
 *
 * <p>claimToken 是持久化 Store 的本次认领凭据，只有匹配令牌的执行者有权确认
 * 发送完成或失败。不再存在缺失 token 的历史确认入口。</p>
 *
 * @param event Outbox 消息快照
 * @param claimToken 本次认领的所有者凭据
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/11 13:31
 */
public record MqOutboxClaim(MqOutboxEvent event, String claimToken) {

    public MqOutboxClaim {
        Objects.requireNonNull(event, "event");
    }
}
