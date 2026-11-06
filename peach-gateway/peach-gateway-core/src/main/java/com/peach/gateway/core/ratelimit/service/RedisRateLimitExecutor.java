package com.peach.gateway.core.ratelimit.service;

import com.peach.gateway.core.GatewayRateLimitRedisConfiguration;
import com.peach.gateway.core.ratelimit.model.RateLimitAlgorithm;
import com.peach.gateway.core.ratelimit.model.RateLimitDecision;
import com.peach.gateway.core.ratelimit.model.RateLimitRule;
import com.peach.gateway.core.ratelimit.script.RateLimitScriptRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Redis Lua 限流算法执行器。
 *
 * <p>每条规则在单次 Lua 调用中原子执行。脚本对象按算法和脚本内容缓存，Nacos 更新脚本后自动替换缓存。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/05
 */
@Component
public class RedisRateLimitExecutor {

    private final ReactiveStringRedisTemplate redisTemplate;

    private final RateLimitScriptRepository scriptRepository;

    private final ConcurrentHashMap<RateLimitAlgorithm, CachedScript> scriptCache = new ConcurrentHashMap<>();

    public RedisRateLimitExecutor(
            @Qualifier(GatewayRateLimitRedisConfiguration.REACTIVE_STRING_REDIS_TEMPLATE_BEAN_NAME)
            ReactiveStringRedisTemplate redisTemplate,
            RateLimitScriptRepository scriptRepository) {
        this.redisTemplate = redisTemplate;
        this.scriptRepository = scriptRepository;
    }

    public Mono<RateLimitDecision> execute(RateLimitRule rule, String key) {
        return execute(rule, key, scriptRepository.get(rule.algorithm()));
    }

    /**
     * 使用请求开始时捕获的脚本源码执行，避免配置刷新期间混用新旧脚本版本。
     *
     * @param rule 限流规则
     * @param key Redis 限流 Key
     * @param scriptSource 请求捕获的脚本源码
     * @return 限流判定结果
     */
    public Mono<RateLimitDecision> execute(RateLimitRule rule, String key, String scriptSource) {
        DefaultRedisScript<List> script = resolveScript(rule.algorithm(), scriptSource);
        return redisTemplate.execute(script, List.of(key), arguments(rule))
                .next()
                .map(this::toDecision)
                .switchIfEmpty(Mono.error(new IllegalStateException("Rate limit Lua script returned no result")));
    }

    private DefaultRedisScript<List> resolveScript(RateLimitAlgorithm algorithm, String source) {
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
            case FIXED_WINDOW -> new Object[]{
                    Integer.toString(rule.limit()), Long.toString(rule.windowMs()), Integer.toString(rule.cost())};
            case SLIDING_WINDOW_LOG -> new Object[]{
                    Integer.toString(rule.limit()), Long.toString(rule.windowMs()),
                    Integer.toString(rule.cost()), UUID.randomUUID().toString()};
            case SLIDING_WINDOW_COUNTER -> new Object[]{
                    Integer.toString(rule.limit()), Long.toString(rule.windowMs()), Integer.toString(rule.cost())};
            case TOKEN_BUCKET -> new Object[]{
                    Double.toString(rule.replenishRate()), Integer.toString(rule.burstCapacity()),
                    Integer.toString(rule.cost())};
            case LEAKY_BUCKET -> new Object[]{
                    Double.toString(rule.leakRate()), Integer.toString(rule.burstCapacity()),
                    Integer.toString(rule.cost())};
            case GCRA -> new Object[]{
                    Double.toString(rule.replenishRate()), Integer.toString(rule.burstCapacity()),
                    Integer.toString(rule.cost())};
        };
    }

    private RateLimitDecision toDecision(List<?> values) {
        if (values.size() < 5) {
            throw new IllegalStateException("Invalid rate limit Lua result");
        }
        return new RateLimitDecision(number(values.get(0)) == 1, number(values.get(1)),
                number(values.get(2)), number(values.get(3)), number(values.get(4)));
    }

    private long number(Object value) {
        try {
            return new BigDecimal(String.valueOf(value)).longValueExact();
        } catch (ArithmeticException | NumberFormatException exception) {
            throw new IllegalStateException("Rate limit Lua result contains a non-integer numeric value", exception);
        }
    }

    private record CachedScript(String source, DefaultRedisScript<List> script) {
    }
}
