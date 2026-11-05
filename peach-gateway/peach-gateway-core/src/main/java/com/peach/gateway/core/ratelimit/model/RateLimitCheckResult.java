package com.peach.gateway.core.ratelimit.model;

/**
 * 一次请求的限流检查结果。
  *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/05
*/
public record RateLimitCheckResult(
        Status status,
        RateLimitRule rule,
        RateLimitDecision decision) {

    public enum Status {
        ALLOWED,
        REJECTED,
        UNAVAILABLE
    }

    public static RateLimitCheckResult allowed() {
        return new RateLimitCheckResult(Status.ALLOWED, null, null);
    }

    public static RateLimitCheckResult allowed(RateLimitRule rule, RateLimitDecision decision) {
        return new RateLimitCheckResult(Status.ALLOWED, rule, decision);
    }

    public static RateLimitCheckResult rejected(RateLimitRule rule, RateLimitDecision decision) {
        return new RateLimitCheckResult(Status.REJECTED, rule, decision);
    }

    public static RateLimitCheckResult unavailable() {
        return new RateLimitCheckResult(Status.UNAVAILABLE, null, null);
    }

    public boolean isAllowed() {
        return status == Status.ALLOWED;
    }
}
