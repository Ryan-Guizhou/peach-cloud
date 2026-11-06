package com.peach.gateway.core.ratelimit;

import com.peach.gateway.core.config.GatewaySaTokenProperties;
import com.peach.gateway.core.ratelimit.config.GatewayRateLimitProperties;
import com.peach.gateway.core.ratelimit.model.RateLimitAlgorithm;
import com.peach.gateway.core.ratelimit.model.RateLimitCheckResult;
import com.peach.gateway.core.ratelimit.model.RateLimitDecision;
import com.peach.gateway.core.ratelimit.model.RateLimitRule;
import com.peach.gateway.core.ratelimit.model.RateLimitScope;
import com.peach.gateway.core.ratelimit.script.RateLimitScriptRepository;
import com.peach.gateway.core.ratelimit.service.RedisRateLimitExecutor;
import com.peach.gateway.core.ratelimit.service.RateLimitEnforcementService;
import com.peach.gateway.core.ratelimit.service.RateLimitKeyFactory;
import com.peach.gateway.core.ratelimit.service.RateLimitMetrics;
import com.peach.gateway.core.ratelimit.service.RateLimitRuleMatcher;
import com.peach.gateway.core.ratelimit.service.RateLimitTenantContextResolver;
import com.peach.gateway.core.ratelimit.service.TrustedProxyClientIpResolver;
import com.peach.gateway.core.ratelimit.service.RateLimitRuleRegistry;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * 使用真实 Redis 验证 Lua 算法原子性和多实例共享限流状态。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/05
 */
@Testcontainers
class RateLimitRedisIntegrationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(RateLimitRedisIntegrationTest.class);

    @Container
    private static final GenericContainer<?> REDIS =
            new GenericContainer<>("redis:7.4-alpine")
                    .withExposedPorts(6379)
                    .withCommand("redis-server", "--save", "", "--appendonly", "no");

    private static LettuceConnectionFactory firstFactory;
    private static LettuceConnectionFactory secondFactory;
    private static ReactiveStringRedisTemplate firstTemplate;
    private static ReactiveStringRedisTemplate secondTemplate;
    private static RedisRateLimitExecutor firstExecutor;
    private static RedisRateLimitExecutor secondExecutor;
    private static RateLimitScriptRepository scriptRepository;

    @BeforeAll
    static void setUp() {
        GatewayRateLimitProperties properties = new GatewayRateLimitProperties();
        scriptRepository = new RateLimitScriptRepository(
                new RateLimitRuleRegistry(properties), new DefaultResourceLoader());
        scriptRepository.reload().block(Duration.ofSeconds(10));

        firstFactory = connectionFactory();
        secondFactory = connectionFactory();
        firstTemplate = new ReactiveStringRedisTemplate(firstFactory);
        secondTemplate = new ReactiveStringRedisTemplate(secondFactory);
        firstExecutor = new RedisRateLimitExecutor(firstTemplate, scriptRepository);
        secondExecutor = new RedisRateLimitExecutor(secondTemplate, scriptRepository);
    }

    @AfterAll
    static void tearDown() {
        if (firstFactory != null) {
            firstFactory.destroy();
        }
        if (secondFactory != null) {
            secondFactory.destroy();
        }
    }

    @Test
    void shouldExecuteEverySupportedAlgorithmAgainstRealRedis() {
        for (RateLimitAlgorithm algorithm : RateLimitAlgorithm.values()) {
            String key = "integration:algorithm:" + algorithm.getConfigKey();
            firstTemplate.delete(key).block(Duration.ofSeconds(3));
            RateLimitRule rule = rule(algorithm, 1, 1);

            RateLimitDecision first = firstExecutor.execute(rule, key).block(Duration.ofSeconds(3));
            RateLimitDecision second = firstExecutor.execute(rule, key).block(Duration.ofSeconds(3));

            assertThat(first).as("first request for %s", algorithm).isNotNull();
            assertThat(first.allowed()).as("first request for %s", algorithm).isTrue();
            assertThat(second).as("second request for %s", algorithm).isNotNull();
            assertThat(second.allowed()).as("second request for %s", algorithm).isFalse();
        }
    }

    @Test
    void shouldShareExactTokenBucketQuotaAcrossIndependentGatewayClients() {
        String key = "integration:multi-instance:token-bucket";
        firstTemplate.delete(key).block(Duration.ofSeconds(3));
        RateLimitRule rule = rule(RateLimitAlgorithm.TOKEN_BUCKET, 25, 25);

        List<RateLimitDecision> decisions = Flux.range(0, 100)
                .flatMap(index -> (index % 2 == 0 ? firstExecutor : secondExecutor)
                        .execute(rule, key), 100)
                .collectList()
                .block(Duration.ofSeconds(15));

        assertThat(decisions).isNotNull().hasSize(100);
        assertThat(decisions.stream().filter(RateLimitDecision::allowed).count()).isEqualTo(25);
        assertThat(decisions.stream().filter(decision -> !decision.allowed()).count()).isEqualTo(75);
    }

    @Test
    void shouldShareQuotaAcrossTwoIndependentGatewayEnforcementInstances() {
        GatewayRateLimitProperties firstProperties = enabledGlobalTokenBucketProperties();
        GatewayRateLimitProperties secondProperties = enabledGlobalTokenBucketProperties();
        RateLimitEnforcementService firstGateway = createGatewayService(firstProperties, firstTemplate);
        RateLimitEnforcementService secondGateway = createGatewayService(secondProperties, secondTemplate);

        List<RateLimitCheckResult> results = Flux.range(0, 100)
                .flatMap(index -> (index % 2 == 0 ? firstGateway : secondGateway)
                        .check(newExchange()), 100)
                .collectList()
                .block(Duration.ofSeconds(20));

        assertThat(results).isNotNull().hasSize(100);
        assertThat(results.stream().filter(RateLimitCheckResult::isAllowed).count()).isEqualTo(25);
        assertThat(results.stream()
                .filter(result -> result.status() == RateLimitCheckResult.Status.REJECTED).count()).isEqualTo(75);
    }

    @Test
    void shouldKeepQuotaExactUnderTenThousandConcurrentRequestsAcrossTwoClients() {
        String key = "integration:stress:ten-thousand";
        firstTemplate.delete(key).block(Duration.ofSeconds(3));
        RateLimitRule rule = new RateLimitRule("integration-stress", true,
                RateLimitScope.GLOBAL, RateLimitAlgorithm.TOKEN_BUCKET, 1,
                Set.of(), List.of(), Set.of(), 1000, Duration.ofSeconds(60).toMillis(),
                0.000001D, 1000, 0.001D, 1);

        long startedAt = System.nanoTime();
        List<TimedDecision> timedDecisions = Flux.range(0, 10_000)
                .flatMap(index -> Mono.defer(() -> {
                    long requestStartedAt = System.nanoTime();
                    RedisRateLimitExecutor executor = index % 2 == 0 ? firstExecutor : secondExecutor;
                    return executor.execute(rule, key)
                            .map(decision -> new TimedDecision(decision, System.nanoTime() - requestStartedAt));
                }), 10_000)
                .collectList()
                .block(Duration.ofSeconds(120));
        long elapsedNanos = System.nanoTime() - startedAt;

        assertThat(timedDecisions).isNotNull().hasSize(10_000);
        long allowed = timedDecisions.stream().filter(item -> item.decision().allowed()).count();
        long rejected = timedDecisions.stream().filter(item -> !item.decision().allowed()).count();
        assertThat(allowed).isEqualTo(1000);
        assertThat(rejected).isEqualTo(9000);

        List<Long> sortedLatencies = timedDecisions.stream()
                .map(TimedDecision::durationNanos)
                .sorted()
                .toList();
        long p95Nanos = sortedLatencies.get((int) Math.ceil(sortedLatencies.size() * 0.95D) - 1);
        long p99Nanos = sortedLatencies.get((int) Math.ceil(sortedLatencies.size() * 0.99D) - 1);
        double throughputRps = timedDecisions.size() / (elapsedNanos / 1_000_000_000D);
        LOGGER.info("Gateway rate-limit load test completed, concurrency={}, totalRequests={}, allowed={}, "
                        + "rejected={}, elapsedMs={}, throughputRps={}, p95Ms={}, p99Ms={}",
                10_000, timedDecisions.size(), allowed, rejected,
                elapsedNanos / 1_000_000D, throughputRps,
                p95Nanos / 1_000_000D, p99Nanos / 1_000_000D);
    }

    @Test
    void shouldSurfaceLuaExecutionErrorsFromRedis() {
        RateLimitScriptRepository brokenRepository = new RateLimitScriptRepository(
                new RateLimitRuleRegistry(new GatewayRateLimitProperties()), new DefaultResourceLoader()) {
            @Override
            public String get(RateLimitAlgorithm algorithm) {
                return "return redis.call('PEACH_UNKNOWN_RATE_LIMIT_COMMAND')";
            }
        };
        RedisRateLimitExecutor brokenExecutor = new RedisRateLimitExecutor(firstTemplate, brokenRepository);

        assertThatThrownBy(() -> brokenExecutor.execute(
                rule(RateLimitAlgorithm.TOKEN_BUCKET, 1, 1), "integration:lua-error")
                .block(Duration.ofSeconds(5)))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void shouldSurfaceRedisConnectionFailure() {
        RedisStandaloneConfiguration configuration =
                new RedisStandaloneConfiguration("127.0.0.1", 1);
        LettuceClientConfiguration clientConfiguration = LettuceClientConfiguration.builder()
                .commandTimeout(Duration.ofMillis(500))
                .build();
        LettuceConnectionFactory unavailableFactory =
                new LettuceConnectionFactory(configuration, clientConfiguration);
        unavailableFactory.afterPropertiesSet();
        try {
            ReactiveStringRedisTemplate unavailableTemplate =
                    new ReactiveStringRedisTemplate(unavailableFactory);
            RedisRateLimitExecutor unavailableExecutor =
                    new RedisRateLimitExecutor(unavailableTemplate, scriptRepository);

            assertThatThrownBy(() -> unavailableExecutor.execute(
                    rule(RateLimitAlgorithm.TOKEN_BUCKET, 1, 1), "integration:redis-unavailable")
                    .block(Duration.ofSeconds(3)))
                    .isInstanceOf(RuntimeException.class);
        } finally {
            unavailableFactory.destroy();
        }
    }

    private record TimedDecision(RateLimitDecision decision, long durationNanos) {
    }

    private GatewayRateLimitProperties enabledGlobalTokenBucketProperties() {
        GatewayRateLimitProperties properties = new GatewayRateLimitProperties();
        properties.setEnabled(true);
        GatewayRateLimitProperties.RuleProperties rule = new GatewayRateLimitProperties.RuleProperties();
        rule.setId("shared-global");
        rule.setEnabled(true);
        rule.setScope(RateLimitScope.GLOBAL);
        rule.setAlgorithm(RateLimitAlgorithm.TOKEN_BUCKET);
        rule.setReplenishRate(0.000001D);
        rule.setBurstCapacity(25);
        rule.setCost(1);
        properties.setRules(List.of(rule));
        return properties;
    }

    private RateLimitEnforcementService createGatewayService(
            GatewayRateLimitProperties properties, ReactiveStringRedisTemplate redisTemplate) {
        RateLimitRuleRegistry registry = new RateLimitRuleRegistry(properties);
        RateLimitScriptRepository repository = new RateLimitScriptRepository(registry, new DefaultResourceLoader());
        repository.reload().block(Duration.ofSeconds(10));
        return new RateLimitEnforcementService(
                registry,
                new RateLimitRuleMatcher(),
                new RateLimitKeyFactory(),
                new RedisRateLimitExecutor(redisTemplate, repository),
                new TrustedProxyClientIpResolver(registry),
                mock(RateLimitTenantContextResolver.class),
                mock(RateLimitMetrics.class),
                repository,
                new GatewaySaTokenProperties());
    }

    private MockServerWebExchange newExchange() {
        return MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/orders/1")
                        .remoteAddress(new InetSocketAddress("203.0.113.10", 8080))
                        .build());
    }

    private static LettuceConnectionFactory connectionFactory() {
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration(
                REDIS.getHost(), REDIS.getMappedPort(6379));
        LettuceConnectionFactory factory = new LettuceConnectionFactory(configuration);
        factory.afterPropertiesSet();
        return factory;
    }

    private RateLimitRule rule(RateLimitAlgorithm algorithm, int limit, int capacity) {
        return new RateLimitRule("integration-" + algorithm.getConfigKey(), true,
                RateLimitScope.GLOBAL, algorithm, 1, Set.of(), List.of(), Set.of(),
                limit, Duration.ofSeconds(60).toMillis(), 0.001D, capacity, 0.001D, 1);
    }
}
