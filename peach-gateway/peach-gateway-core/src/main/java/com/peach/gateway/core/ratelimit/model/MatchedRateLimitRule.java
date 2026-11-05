package com.peach.gateway.core.ratelimit.model;

/**
 * 当前请求命中的限流规则及其规范化路径模式。
 *
 * @param rule 限流规则
 * @param matchedPathPattern 命中的 URI 模式
  *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/05
*/
public record MatchedRateLimitRule(RateLimitRule rule, String matchedPathPattern) {
}
