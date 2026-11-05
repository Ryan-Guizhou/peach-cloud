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
import java.util.concurrent.ConcurrentHashMap;

/**
 * Redis Lua 限流算法执行器。
 *
 * <p>每条规则在单次 Lua 调用中原子执行。多条规则按优先级顺序执行；
 * 若后续规则拒绝，之前已扣减的额度不回滚，避免跨 Key 分布式事务。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/05
 */
@Component
@RequiredArgsConstructor
public class RedisRateLimitExecutor {

    private final ReactiveStringRedisTemplate redisTemplate;
    private final RateLimitScriptRepository scriptRepository;

    private final ConcurrentHashMap<RateLimitAlgorithm, CachedScript> scriptCache = new ConcurrentHashMap<>();

    public Mono<RateLimitDecision> execute(RateLimitRule rule, String key) {
        DefaultRedisScript<List> script = resolveScript(rule.algorithm());
        List<String> keys = rule.algorithm() == RateLimitAlgorithm.SLIDING_WINDOW_COUNTER
                ? List.of(key, key) : List.of(key);
        return redisTemplate.execute(script, keys, arguments(rule))
                .next()
                .map(this::toDecision)
                .switchIfEmpty(Mono.error(new IllegalStateException("Rate limit Lua script returned no result")));
    }

    private DefaultRedisScript<List> resolveScript(RateLimitAlgorithm algorithm) {
        String source = scriptRepository.get(algorithm);
        CachedScript cached = scriptCache.compute(algorithm, (key, current) -> {
            if (current != null && current.source().equals(source)) {
                return current;
            }
            return new CachedScript(source, new DefaultRedisScript<>(source, List.class));
        });
        return cached.script();
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

    private record CachedScript(String source, DefaultRedisScript<List> script) {
    }
}
