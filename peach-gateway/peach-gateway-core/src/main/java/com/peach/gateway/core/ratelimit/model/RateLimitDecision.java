package com.peach.gateway.core.ratelimit.model;

/**
 * Redis 限流脚本返回的单条规则决策。
 *
 * @param allowed 是否允许当前请求
 * @param remaining 当前剩余额度
 * @param resetAfterMs 额度完全恢复或窗口重置所需毫秒数
 * @param retryAfterMs 被拒绝后建议等待的毫秒数
 * @param limit 当前规则的额度上限
 */
public record RateLimitDecision(
        boolean allowed,
        long remaining,
        long resetAfterMs,
        long retryAfterMs,
        long limit) {
}
