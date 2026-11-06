package com.peach.gateway.core.ratelimit.service;

import com.peach.gateway.core.ratelimit.constant.GatewayRateLimitConstants;
import com.peach.gateway.core.ratelimit.model.MatchedRateLimitRule;
import com.peach.gateway.core.ratelimit.model.RateLimitAlgorithm;
import com.peach.gateway.core.ratelimit.model.RateLimitRule;
import com.peach.gateway.core.ratelimit.model.RateLimitScope;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 限流 Redis Key 版本隔离测试。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/06
 */
class RateLimitKeyFactoryTest {

    @Test
    void shouldUseDifferentRedisKeysWhenRuleAlgorithmChanges() {
        RateLimitKeyFactory factory = new RateLimitKeyFactory();
        RateLimitRule tokenBucket = rule(RateLimitAlgorithm.TOKEN_BUCKET);
        RateLimitRule gcra = rule(RateLimitAlgorithm.GCRA);

        String tokenBucketKey = factory.create(new MatchedRateLimitRule(tokenBucket, ""),
                null, null, "GET", "/api/orders");
        String gcraKey = factory.create(new MatchedRateLimitRule(gcra, ""),
                null, null, "GET", "/api/orders");

        assertThat(tokenBucketKey).isNotEqualTo(gcraKey);
        assertThat(tokenBucketKey).contains(":token-bucket:");
        assertThat(gcraKey).contains(":gcra:");
        assertThat(tokenBucketKey).startsWith(GatewayRateLimitConstants.REDIS_KEY_PREFIX);
    }

    private RateLimitRule rule(RateLimitAlgorithm algorithm) {
        return new RateLimitRule("orders", true, RateLimitScope.GLOBAL, algorithm,
                1, Set.of(), List.of(), Set.of(), 100, 1000, 100D, 100, 100D, 1);
    }
}
