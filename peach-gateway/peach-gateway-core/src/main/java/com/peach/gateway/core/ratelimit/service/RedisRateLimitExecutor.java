package com.peach.gateway.core.ratelimit.service;

import com.peach.gateway.core.ratelimit.model.RateLimitAlgorithm;
import com.peach.gateway.core.ratelimit.model.RateLimitDecision;
import com.peach.gateway.core.ratelimit.model.RateLimitRule;
import com.peach.gateway.core.ratelimit.script.RateLimitScriptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

/**
 * Redis Lua 限流算法执行器。
 *
 * <p>每条规则在单次 Lua 调用中原子执行。多条规则按优先级顺序执行；
 * 若后续规则拒绝，之前已扣减的额度不回滚，避免跨 Key 分布式事务。</p>
 */
@Component
@RequiredArgsConstructor
public class RedisRateLimitExecutor {

    private final ReactiveStringRedisTemplate redisTemplate;
    private final RateLimitScriptRepository scriptRepository;

    public Mono<RateLimitDecision> execute(RateLimitRule rule, String key) {
        String source = scriptRepository.get(rule.algorithm());
        DefaultRedisScript<List> script = new DefaultRedisScript<>(source, List.class);
        List<String> keys = rule.algorithm() == RateLimitAlgorithm.SLIDING_WINDOW_COUNTER
                ? List.of(key, key) : List.of(key);
        return redisTemplate.execute(script, keys, arguments(rule))
                .next()
                .map(this::toDecision)
                .switchIfEmpty(Mono.error(new IllegalStateException("Rate limit Lua script returned no result")));
    }

    private Object[] arguments(RateLimitRule rule) {
        return switch (rule.algorithm()) {
            case FIXED_WINDOW -> new Object[]{rule.limit(), rule.windowMs(), rule.cost()};
            case SLIDING_WINDOW_LOG -> new Object[]{rule.limit(), rule.windowMs(), rule.cost(), UUID.randomUUID().toString()};
            case SLIDING_WINDOW_COUNTER -> new Object[]{rule.limit(), rule.windowMs(), rule.cost()};
            case TOKEN_BUCKET -> new Object[]{rule.replenishRate(), rule.burstCapacity(), rule.cost()};
            case LEAKY_BUCKET -> new Object[]{rule.leakRate(), rule.burstCapacity(), rule.cost()};
            case GCRA -> new Object[]{rule.replenishRate(), rule.burstCapacity(), rule.cost()};
        };
    }

    private RateLimitDecision toDecision(List values) {
        if (values.size() < 5) {
            throw new IllegalStateException("Invalid rate limit Lua result");
        }
        return new RateLimitDecision(number(values.get(0)) == 1, number(values.get(1)),
                number(values.get(2)), number(values.get(3)), number(values.get(4)));
    }

    private long number(Object value) {
        return Long.parseLong(String.valueOf(value));
    }
}
